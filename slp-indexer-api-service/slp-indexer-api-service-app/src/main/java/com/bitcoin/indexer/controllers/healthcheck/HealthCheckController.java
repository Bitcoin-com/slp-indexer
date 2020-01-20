package com.bitcoin.indexer.controllers.healthcheck;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.bitcoin.indexer.blockchain.domain.slp.SlpTokenDetails;
import com.bitcoin.indexer.blockchain.domain.slp.SlpTokenId;
import com.bitcoin.indexer.repository.SlpDetailsRepository;
import com.bitcoin.indexer.responses.HealthCheckResponse;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Request.Builder;
import okhttp3.Response;

@RestController
@RequestMapping("v1/health")
public class HealthCheckController {

	private static final Logger logger = LoggerFactory.getLogger(HealthCheckController.class);
	private SlpDetailsRepository detailsRepository;
	private OkHttpClient okHttpClient = new OkHttpClient.Builder().build();

	private static SlpTokenId DUMMY_TOKEN_FAILOVER = new SlpTokenId("7f8889682d57369ed0e32336f8b7e0ffec625a35cca183f4e81fde4e71a538a1");

	@GetMapping("")
	public HealthCheckResponse healthCheck(@Value("${region}") String region, @Value("${writer.ip:}") String writerIp, HttpServletRequest httpRequest) {
		try {
			SlpTokenDetails slpTokenDetails = detailsRepository.fetchSlpDetails(DUMMY_TOKEN_FAILOVER).blockingGet();
			if (slpTokenDetails == null) {
				throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE);
			}
		} catch (Exception e) {
			throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE);
		}

		if (!writerIp.isBlank()) {
			Request request = new Builder()
					.url(writerIp + "/api/v1/health")
					.get()
					.build();
			try {
				Response execute = okHttpClient.newCall(request).execute();
				if (!execute.isSuccessful()) {
					logger.error("Writer is not responding will assume it is not working correctly failover");
					throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE);
				}
			} catch (Exception e) {
				logger.error("Writer is not responding will assume it is not working correctly failover");
				throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE);
			}
		}

		return new HealthCheckResponse(region, getClientIpAddr(httpRequest));
	}

	public HealthCheckController(SlpDetailsRepository detailsRepository) {
		this.detailsRepository = detailsRepository;
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