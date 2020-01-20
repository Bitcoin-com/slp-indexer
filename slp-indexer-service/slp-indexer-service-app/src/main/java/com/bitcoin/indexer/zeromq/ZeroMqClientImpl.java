package com.bitcoin.indexer.zeromq;

import java.util.Objects;

import org.bitcoinj.core.BitcoinSerializer;
import org.bitcoinj.core.Block;
import org.bitcoinj.core.Context;
import org.bitcoinj.core.PeerGroup;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.params.MainNetParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ.Socket;

public class ZeroMqClientImpl implements Runnable {

	private static final Logger logger = LoggerFactory.getLogger(ZeroMqClientImpl.class);
	private static final String RAW_BLOCK_TOPIC = "rawblock";
	private static final String RAW_TRANSACTION_TOPIC = "rawblock";

	private final String hostname;
	private final int port;
	private final RawBlockReader rawBlockReader;
	private final RawTxReader rawTxReader;
	private final Runnable bitcoinJLocalThreadContextInitializer;

	private PeerGroup peerGroup;

	private final ZeroMqSubscription subscription;

	private volatile boolean destroyed = false;

	private interface RawBlockReader {
		Block read(byte[] bytes);
	}

	private interface RawTxReader {
		Transaction read(byte[] bytes);
	}

	public interface ZeroMqSubscription {
		void block(Block block);

		void transaction(Transaction transaction);
	}

	public ZeroMqClientImpl(String hostname, int port, ZeroMqSubscription subscription) {
		this.hostname = Objects.requireNonNull(hostname);
		this.port = port;
		this.subscription = Objects.requireNonNull(subscription);
		bitcoinJLocalThreadContextInitializer = () -> Context.propagate(Context.get());
		rawBlockReader = bytes -> new BitcoinSerializer(MainNetParams.get(), true).makeBlock(bytes);
		rawTxReader = bytes -> new BitcoinSerializer(MainNetParams.get(), true).makeTransaction(bytes);
	}

	private Socket socket = null;

	void destroy() {
		destroyed = true;
		if (socket != null) {
			socket.unsubscribe(RAW_BLOCK_TOPIC);
			socket.close();
		}
	}

	@Override
	public void run() {
		bitcoinJLocalThreadContextInitializer.run();
		while (true) {
			if (destroyed) {
				return;
			}
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				return;
			}
			try (ZContext context = new ZContext()) {
				socket = context.createSocket(SocketType.SUB);
				boolean connected = socket.connect(String.format("tcp://%s:%d", hostname, port));
				logger.info("host={} port={} connected={}", hostname, port, connected);
				socket.subscribe(RAW_BLOCK_TOPIC);
				while (true) {
					try {
						String topic = socket.recvStr();
						logger.trace("recvStr={}", topic);
						byte[] rawBlock = socket.recv();
						Block block = rawBlockReader.read(rawBlock);
						subscription.block(block);
						String multiPartTerminator = socket.recvStr();
						logger.trace("recvStr={}", multiPartTerminator);
					} catch (RuntimeException e) {
						if (!destroyed) {
							logger.error("Failed to handle ZeroMQ data", e);
						}
						break; // Out of sync. Fall through and reconnect
					}
				}
			} catch (RuntimeException e) {
				logger.error("ZeroMq error", e);
			} finally {
				if (socket != null) {
					socket.unsubscribe(RAW_BLOCK_TOPIC);
					socket.close();
				}
				logger.info("Disconnected from host={} port={}", hostname, port);
			}
		}
	}
}