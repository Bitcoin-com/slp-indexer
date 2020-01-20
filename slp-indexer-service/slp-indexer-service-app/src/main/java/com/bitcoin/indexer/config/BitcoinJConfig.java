package com.bitcoin.indexer.config;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.bitcoinj.core.Context;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.PeerGroup;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.store.BlockStore;
import org.bitcoinj.store.BlockStoreException;
import org.bitcoinj.store.SPVBlockStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import com.bitcoin.indexer.core.Network;
import com.bitcoin.indexer.listener.BitcoinJListener;
import com.bitcoin.indexer.repository.BlockRepository;

@Configuration
public class BitcoinJConfig implements DisposableBean {

	private static final Logger logger = LoggerFactory.getLogger(BitcoinJConfig.class);

	public static Network network;

	private BlockchainExtended blockChain;

	private PeerGroup peerGroup;

	@Autowired
	public BlockRepository blockRepository;

	@Bean
	public BlockStore blockStore(Environment environment, NetworkParameters networkParameters) throws Exception {
		String property = environment.getProperty("svp.header.store");
		if (property == null) {
			throw new RuntimeException("Must point to location of spv");
		}
		logger.info("using spv file={}", property);

		return new SPVBlockStore(networkParameters, new File(property));
	}

	@Bean
	public NetworkParameters networkParameters() {
		network = Network.LIVENET;
		MainNetParams mainNetParams = MainNetParams.get();
		List<String> bitcoinJseeds = Arrays.asList(mainNetParams.getDnsSeeds());
		if (bitcoinJseeds.contains("seed-abc.bitcoinforks.org")) {
			logger.info("Running against BCH chain with seeds={}", bitcoinJseeds);
		}
		if (bitcoinJseeds.contains("seed.bitcoin.sipa.be")) {
			logger.info("Running against BTC chain with seeds={}", bitcoinJseeds);
		}

		return mainNetParams;
	}

	@Bean
	public Context context(NetworkParameters networkParameters) {
		Context context = new Context(networkParameters);
		Context.propagate(context);
		return context;
	}

	@Bean
	public BlockchainExtended blockChain(BlockStore blockStore, Context context, BitcoinJListener bitcoinJListener) throws BlockStoreException {
		this.blockChain = new BlockchainExtended(context, blockStore, bitcoinJListener);
		return blockChain;
	}

	@Bean
	public PeerGroup peerGroup(Context context, BlockchainExtended blockChain) {
		peerGroup = new PeerGroup(context, blockChain);
		return peerGroup;
	}

	@Override
	public void destroy() throws Exception {
		try {
			peerGroup.stopAsync().get(10, TimeUnit.SECONDS);
		} catch (RuntimeException e) {
			logger.error("Failed to stop PeerGroup", e);
		} finally {
			BlockStore blockStore = blockChain.getBlockStore();
			blockStore.close();
		}
	}
}
