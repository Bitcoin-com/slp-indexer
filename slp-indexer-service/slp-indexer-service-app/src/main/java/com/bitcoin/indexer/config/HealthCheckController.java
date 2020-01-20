package com.bitcoin.indexer.config;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.servlet.http.HttpServletRequest;

import org.bitcoinj.core.PeerGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.bitcoin.indexer.listener.BitcoinJListener;

import io.reactivex.schedulers.Schedulers;

@RestController
@RequestMapping("v1/health")
public class HealthCheckController {

	private final BitcoinJListener bitcoinJListener;
	private PeerGroup peerGroup;
	private static final Logger logger = LoggerFactory.getLogger(HealthCheckController.class);
	private Instant sinceLastTx = Instant.now();

	public HealthCheckController(BitcoinJListener bitcoinJListener, PeerGroup peerGroup) {
		this.bitcoinJListener = Objects.requireNonNull(bitcoinJListener);
		this.peerGroup = Objects.requireNonNull(peerGroup);
		ExecutorService service = Executors.newSingleThreadExecutor();
		bitcoinJListener.getTransactionFlowable().subscribeOn(Schedulers.from(service)).subscribe(tx -> {
					sinceLastTx = Instant.now();
				}
		);
	}

	@GetMapping("")
	public HealthCheckResponse healthCheck(@Value("${region}") String region, HttpServletRequest httpRequest) {
		if (peerGroup.getConnectedPeers().isEmpty()) {
			logger.error("No connected peers. Writer is not listening to blockchain");
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
		}
		Instant now = Instant.now();
		if (Duration.between(sinceLastTx, now).toMinutes() > 3) {
			logger.error("No transactions registered in 5 minutes will assume failover sinceLast={} now={}", sinceLastTx, now);
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
		}

		return new HealthCheckResponse(region, getClientIpAddr(httpRequest));
	}

	private static final List<String> IP_HEADERS = Arrays.asList("X-Forwarded-For",
			"Proxy-Client-IP",
			"WL-Proxy-Client-IP",
			"HTTP_CLIENT_IP",
			"HTTP_X_FORWARDED_FOR");

	public static String getClientIpAddr(HttpServletRequest request) {
		return IP_HEADERS.stream()
				.map(request::getHeader)
				.filter(Objects::nonNull)
				.filter(ip -> !ip.isEmpty() && !ip.equalsIgnoreCase("unknown"))
				.findFirst()
				.orElseGet(request::getRemoteAddr);
	}
}