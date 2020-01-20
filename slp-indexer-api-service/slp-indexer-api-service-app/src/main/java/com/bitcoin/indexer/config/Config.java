package com.bitcoin.indexer.config;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;

import com.bitcoin.indexer.core.Coin;
import com.bitcoin.indexer.facade.InsightsFacade;
import com.bitcoin.indexer.facade.InsightsFacadeImpl;
import com.bitcoin.indexer.repository.SlpDetailsRepository;
import com.bitcoin.indexer.repository.SlpDetailsRepositoryImpl;
import com.bitcoin.indexer.repository.TransactionRepository;
import com.bitcoin.indexer.repository.TransactionRepositoryImpl;
import com.bitcoin.indexer.repository.UtxoRepository;
import com.bitcoin.indexer.repository.UtxoRepositoryImpl;
import com.mongodb.MongoClientOptions;

import okhttp3.OkHttpClient;

@Configuration
public class Config {

	private static final Logger logger = LoggerFactory.getLogger(Config.class);

	@Bean
	public Coin coin() {
		return Coin.BCH;
	}

	@Bean
	public MongoClientOptions mongoOptions() {
		return MongoClientOptions.builder()
				.connectionsPerHost(15000)
				.threadsAllowedToBlockForConnectionMultiplier(3000)
				.build();
	}

	@Bean
	public TransactionRepository transactionRepository(ReactiveMongoTemplate reactiveMongoTemplate, MongoOperations mongoOperations, Coin coin) {
		return new TransactionRepositoryImpl(mongoOperations, reactiveMongoTemplate, coin);
	}

	@Bean
	public InsightsFacade insightsFacade(@Value("${insights.bch.live}") String insightsIP) {
		OkHttpClient okHttpClient = new OkHttpClient.Builder().readTimeout(30, TimeUnit.SECONDS).build();
		return new InsightsFacadeImpl(okHttpClient, insightsIP);
	}

	@Bean
	public UtxoRepository utxoRepository(ReactiveMongoTemplate reactiveMongoTemplate, MongoOperations mongoOperations) {
		return new UtxoRepositoryImpl(false, reactiveMongoTemplate, mongoOperations, 1000);
	}

	@Bean
	public SlpDetailsRepository detailsRepository(ReactiveMongoTemplate reactiveMongoTemplate) {
		return new SlpDetailsRepositoryImpl(reactiveMongoTemplate);
	}
}
