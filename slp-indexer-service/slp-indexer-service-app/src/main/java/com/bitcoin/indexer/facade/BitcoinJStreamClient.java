package com.bitcoin.indexer.facade;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.bitcoinj.core.Context;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Peer;
import org.bitcoinj.core.PeerGroup;
import org.bitcoinj.net.discovery.DnsDiscovery;
import org.bitcoinj.net.discovery.PeerDiscovery;
import org.bitcoinj.net.discovery.PeerDiscoveryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;

import com.bitcoin.indexer.config.BlockchainExtended;
import com.bitcoin.indexer.core.Coin;
import com.bitcoin.indexer.listener.BitcoinJListener;

public class BitcoinJStreamClient {

	private static final Logger logger = LoggerFactory.getLogger(BitcoinJStreamClient.class);

	private final PeerGroup peerGroup;
	private final BitcoinJListener listener;
	private final NetworkParameters networkParameters;

	public BitcoinJStreamClient(Environment environment,
			BitcoinJListener listener,
			NetworkParameters networkParameters,
			PeerGroup peerGroup,
			Context context,
			BlockchainExtended blockChain,
			Coin coin) {
		this.listener = listener;
		this.networkParameters = networkParameters;
		this.peerGroup = peerGroup;
		try {
			String bitcoinjType = environment.getProperty("bitcoinj.type");
			String bitcoinJVersion = environment.getProperty("bitcoinj.version");
			if (bitcoinjType == null || bitcoinJVersion == null) {
				throw new RuntimeException("Need bitcoinj.type and bitcoinj.version properties");
			}
			logger.info("User agent {} {}", bitcoinjType, bitcoinJVersion);
			peerGroup.setUserAgent(bitcoinjType, bitcoinJVersion);
			peerGroup.startAsync();
			peerGroup.addPeerDiscovery(configPeers(environment.getProperty("bitcoinj.peers." + coin.name().toLowerCase(), String.class, ""), context.getParams(), environment));
			peerGroup.setMaxConnections(environment.getProperty(
					"bitcoinj.max.connection",
					Integer.TYPE,
					2));
			logger.info("Waiting for peers....");
			peerGroup.waitForPeers(environment.getProperty(
					"bitcoinj.awaiting.peer",
					Integer.TYPE,
					2)).get(10, TimeUnit.MINUTES);
			logger.info("Done waiting for peers");

			peerGroup.setFastCatchupTimeSecs(1534252200);

			peerGroup.startBlockChainDownload(listener);

			blockChain.addNewBestBlockListener(listener);

			peerGroup.addBlocksDownloadedEventListener(listener);

			Executors.newSingleThreadExecutor().submit(() -> {
				while (true) {
					try {

						int height = peerGroup.getConnectedPeers().stream()
								.map(Peer::getBestHeight)
								.max(Long::compareTo).orElse(peerGroup.getDownloadPeer().getBestHeight())
								.intValue();
						int blockstoreHeight = blockChain.getBestChainHeight();
						if (height == blockstoreHeight) {
							break;
						}
						Thread.sleep(200);
					} catch (Exception e) {
						throw new RuntimeException("Could not start listener....");
					}
				}
				logger.info("Blockstore and blockchain is in sync adding txlistener...");
				peerGroup.addOnTransactionBroadcastListener(Executors.newSingleThreadExecutor(), listener);
			});

		} catch (Exception e) {
			throw new RuntimeException("Could not start bitcoinj", e);
		}
	}

	private PeerDiscovery configPeers(String peers, NetworkParameters networkParameters, Environment environment) {
		Set<String> profiles = new HashSet<>(Arrays.asList(environment.getActiveProfiles()));

		if (profiles.contains("localhost")) {
			logger.info("Not using any configured peers only bitcoinj peers...");
			return new DnsDiscovery(networkParameters);
		}

		String dnsSeed = environment.getProperty("dns.discovery", "");

		logger.info("Configured peers={}", peers);

		InetSocketAddress[] ourNodes = Stream.of(peers.split(","))
				.map(s -> s.split(":"))
				.map(adr -> new InetSocketAddress(adr[0], Integer.valueOf(adr[1])))
				.toArray(InetSocketAddress[]::new);
		DnsDiscovery dnsDiscovery = getDnsSeeds(networkParameters, dnsSeed);

		return new PeerDiscovery() {
			@Override
			public InetSocketAddress[] getPeers(long services, long timeoutValue, TimeUnit timeoutUnit) {
				try {
					InetSocketAddress[] bitcoinj = dnsDiscovery.getPeers(0, 2, TimeUnit.MINUTES);
					return Stream.concat(Arrays.stream(ourNodes), Arrays.stream(bitcoinj))
							.toArray(InetSocketAddress[]::new);
				} catch (PeerDiscoveryException e) {
					logger.error("Could not call bitcoinj only using our nodes");
					return ourNodes;
				}
			}

			@Override
			public void shutdown() {

			}
		};
	}

	private DnsDiscovery getDnsSeeds(NetworkParameters networkParameters, String dnsSeed) {
		if (dnsSeed.isEmpty()) {
			return new DnsDiscovery(networkParameters);
		}

		return new DnsDiscovery(new String[] { dnsSeed }, networkParameters);
	}
}
