package com.bitcoin.indexer.config;

import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import org.bitcoinj.core.Context;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.PeerGroup;
import org.bitcoinj.store.BlockStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.ReactiveMongoOperations;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;

import com.bitcoin.indexer.core.Coin;
import com.bitcoin.indexer.facade.BitcoinJConverters;
import com.bitcoin.indexer.facade.BitcoinJStreamClient;
import com.bitcoin.indexer.facade.validators.GenesisValidatorAssumeParentValid;
import com.bitcoin.indexer.facade.validators.MintValidatorAssumeParentValid;
import com.bitcoin.indexer.facade.validators.SendValidatorAssumeParentValid;
import com.bitcoin.indexer.facade.validators.SlpValidatorCustomImplAssumeParentValid;
import com.bitcoin.indexer.facade.validators.SlpValidatorFacade;
import com.bitcoin.indexer.handlers.BlockHandler;
import com.bitcoin.indexer.handlers.BlockHandlerImpl;
import com.bitcoin.indexer.handlers.InputHandler;
import com.bitcoin.indexer.handlers.InputHandlerImpl;
import com.bitcoin.indexer.handlers.TransactionHandler;
import com.bitcoin.indexer.handlers.TransactionHandlerSlpImpl;
import com.bitcoin.indexer.handlers.UtxoHandler;
import com.bitcoin.indexer.handlers.UtxoHandlerImpl;
import com.bitcoin.indexer.listener.BitcoinJListener;
import com.bitcoin.indexer.repository.BlockRepository;
import com.bitcoin.indexer.repository.BlockRepositoryImpl;
import com.bitcoin.indexer.repository.SlpDetailsRepository;
import com.bitcoin.indexer.repository.SlpDetailsRepositoryImpl;
import com.bitcoin.indexer.repository.SlpVerifiedTokenRepository;
import com.bitcoin.indexer.repository.SlpVerifiedTokenRepositoryImpl;
import com.bitcoin.indexer.repository.TransactionRepository;
import com.bitcoin.indexer.repository.TransactionRepositoryImpl;
import com.bitcoin.indexer.repository.UtxoRepository;
import com.bitcoin.indexer.repository.UtxoRepositoryImpl;

import io.micrometer.core.instrument.Metrics;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import okhttp3.OkHttpClient;

@Configuration
public class Config {

	private static final Logger logger = LoggerFactory.getLogger(Config.class);

	@Bean
	public OkHttpClient okHttpClient() {
		return new OkHttpClient.Builder()
				.readTimeout(3, TimeUnit.SECONDS)
				.build();
	}

	@Bean
	public BitcoinJStreamClient blockchainClient(Environment environment,
			BitcoinJListener listener,
			NetworkParameters networkParameters,
			Context context,
			PeerGroup peerGroup,
			BlockchainExtended blockChain,
			Coin coin) {
		return new BitcoinJStreamClient(
				environment,
				listener,
				networkParameters,
				peerGroup,
				context,
				blockChain,
				coin);
	}

	@Bean
	public Coin coin(@Value("${blockchain.coin:BCH}") String coin) {
		Coin value = Coin.valueOf(coin.toUpperCase());
		logger.info("Running for coin={}", value);
		return value;
	}

	@Bean
	public BitcoinJConverters bitcoinJConverters(SlpDetailsRepository slpTokenDetailsFacade,
			Coin coin,
			UtxoRepository utxoRepository,
			BlockRepository blockRepository) {
		return new BitcoinJConverters(slpTokenDetailsFacade, utxoRepository, coin, blockRepository);
	}

	@Bean
	public BitcoinJListener listener(TransactionHandler transactionHandler,
			BlockStore blockStore,
			NetworkParameters networkParameters,
			Coin coin,
			BitcoinJConverters bitcoinJConverters,
			BlockHandler blockHandler,
			UtxoRepository utxoRepository,
			@Value("${is.full.mode:false}") String isFullMode) {
		return new BitcoinJListener(transactionHandler, networkParameters, blockHandler, blockStore, coin, bitcoinJConverters, Boolean.parseBoolean(isFullMode), utxoRepository);
	}

	@Bean
	public BlockHandler blockHandler(BlockRepository blockRepository) {
		return new BlockHandlerImpl(blockRepository);
	}

	@Bean
	public BlockRepository blockRepository(ReactiveMongoOperations reactiveMongoOperations) {
		return new BlockRepositoryImpl(reactiveMongoOperations);
	}

	@Bean
	public UtxoHandler utxoHandler(UtxoRepository utxoRepository, Coin coin) {
		return new UtxoHandlerImpl(utxoRepository, coin);
	}

	@Bean
	public TransactionHandler transactionHandler(InputHandler inputHandler,
			UtxoHandler utxoHandler,
			TransactionRepository transactionRepository,
			SlpValidatorFacade slpValidatorFacade) {
		return new TransactionHandlerSlpImpl(
				inputHandler,
				utxoHandler,
				transactionRepository,
				slpValidatorFacade);
	}

	@Bean
	public InputHandler inputHandler(UtxoRepository utxoRepository, Coin coin) {
		return new InputHandlerImpl(utxoRepository, coin);
	}

	@Bean
	public UtxoRepository utxoRepository(ReactiveMongoOperations reactiveMongoOperations,
			MongoOperations mongoOperations,
			@Value("${initial.sync:true}") String initislSync,
			@Value("${cache.size:100000}") String cacheSize) {
		return new UtxoRepositoryImpl(Boolean.parseBoolean(initislSync), reactiveMongoOperations, mongoOperations, Integer.parseInt(cacheSize));
	}

	@Bean
	public TransactionRepository transactionRepository(MongoOperations mongoOperations,
			Coin coin, ReactiveMongoTemplate reactiveMongoTemplate) {
		return new TransactionRepositoryImpl(mongoOperations, reactiveMongoTemplate, coin);
	}

	@Bean
	public SlpDetailsRepository slpDetailsRepository(ReactiveMongoOperations reactiveMongoOperations) {
		return new SlpDetailsRepositoryImpl(reactiveMongoOperations);
	}

	@Bean
	public MintValidatorAssumeParentValid mintValidator(TransactionRepository transactionRepository, UtxoRepository utxoRepository) {
		return new MintValidatorAssumeParentValid(transactionRepository, utxoRepository);
	}

	@Bean
	public GenesisValidatorAssumeParentValid genesisValidator(TransactionRepository transactionRepository, UtxoRepository utxoRepository) {
		return new GenesisValidatorAssumeParentValid(transactionRepository, utxoRepository);
	}

	@Bean
	public SendValidatorAssumeParentValid sendValidator(TransactionRepository transactionRepository, UtxoRepository utxoRepository) {
		return new SendValidatorAssumeParentValid(transactionRepository, utxoRepository);
	}

	@Bean
	public SlpValidatorFacade slpValidatorFacade(TransactionRepository transactionRepository,
			MintValidatorAssumeParentValid mintValidator, GenesisValidatorAssumeParentValid genesisValidator, SendValidatorAssumeParentValid sendValidator) {
		return new SlpValidatorCustomImplAssumeParentValid(transactionRepository, mintValidator, sendValidator, genesisValidator);
	}

	@Bean
	public SlpVerifiedTokenRepository slpVerifiedTokenRepository(ReactiveMongoOperations reactiveMongoOperations) {
		return new SlpVerifiedTokenRepositoryImpl(reactiveMongoOperations);
	}
	

	@Autowired
	PrometheusMeterRegistry registry;

	@PostConstruct
	public void afterStart() {
		Metrics.addRegistry(registry);
	}

}
