package com.bitcoin.indexer;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

import org.bitcoinj.core.BitcoinSerializer;
import org.bitcoinj.core.Block;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Peer;
import org.bitcoinj.core.PeerGroup;
import org.bitcoinj.core.Transaction;
import org.bouncycastle.util.encoders.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import com.bitcoin.indexer.facade.BitcoinJConverters;
import com.bitcoin.indexer.listener.BitcoinJListener;

@RequestMapping("/admin")
public class AdminController {

	private final BitcoinJListener bitcoinJListener;
	private final NetworkParameters networkParameters;
	private BitcoinJConverters bitcoinJConverters;
	private PeerGroup peerGroup;
	private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

	public AdminController(BitcoinJListener bitcoinJListener,
			NetworkParameters networkParameters,
			BitcoinJConverters bitcoinJConverters,
			PeerGroup peerGroup) {
		this.bitcoinJListener = Objects.requireNonNull(bitcoinJListener);
		this.networkParameters = Objects.requireNonNull(networkParameters);
		this.bitcoinJConverters = Objects.requireNonNull(bitcoinJConverters);
		this.peerGroup = peerGroup;
	}

	@PostMapping("/block")
	public ResponseEntity reindexBlock(@RequestBody ReindexBlockRequest request) {
		byte[] block = Hex.decode(request.rawBlock);

		Block reindexBlock = new BitcoinSerializer(networkParameters, true).makeBlock(block);

		com.bitcoin.indexer.blockchain.domain.Block consumeBlock = bitcoinJConverters.block(reindexBlock, request.height, networkParameters);

		this.bitcoinJListener.consumeTransactions(consumeBlock.getTransactions());
		return ResponseEntity.ok().build();
	}

	@PostMapping("/tx")
	public ResponseEntity reindexTx(@RequestBody ReindexTxRequest request) {
		logger.info("Admin reindexing tx");

		byte[] rawTx = Hex.decode(request.rawTx);
		Peer peer = peerGroup.getConnectedPeers().stream().findFirst().orElse(peerGroup.getDownloadPeer());
		Transaction t = new Transaction(networkParameters, rawTx);
		com.bitcoin.indexer.blockchain.domain.Transaction transaction = bitcoinJConverters.transaction(t,
				networkParameters,
				Long.valueOf(peer.getBestHeight()).intValue(),
				false,
				"",
				Instant.now(),
				null
		);
		this.bitcoinJListener.consumeTransactions(List.of(transaction));

		return ResponseEntity.ok().build();
	}

	public static class ReindexBlockRequest {
		private String rawBlock;
		private int height;

		public ReindexBlockRequest() {
		}

		public String getRawBlock() {
			return rawBlock;
		}

		public int getHeight() {
			return height;
		}
	}

	public static class ReindexTxRequest {
		private String rawTx;

		public ReindexTxRequest() {
		}

		public String getRawTx() {
			return rawTx;
		}
	}
}
