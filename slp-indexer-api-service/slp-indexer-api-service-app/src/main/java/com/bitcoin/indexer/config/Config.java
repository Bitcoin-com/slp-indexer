package com.bitcoin.indexer.config;

import java.util.concurrent.TimeUnit;

import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.index.CompoundIndexDefinition;
import org.springframework.data.mongodb.core.index.Index;

import com.bitcoin.indexer.core.Coin;
import com.bitcoin.indexer.facade.InsightsFacade;
import com.bitcoin.indexer.facade.InsightsFacadeImpl;
import com.bitcoin.indexer.repository.SlpDetailsRepository;
import com.bitcoin.indexer.repository.SlpDetailsRepositoryImpl;
import com.bitcoin.indexer.repository.TransactionRepository;
import com.bitcoin.indexer.repository.TransactionRepositoryImpl;
import com.bitcoin.indexer.repository.UtxoRepository;
import com.bitcoin.indexer.repository.UtxoRepositoryImpl;
import com.bitcoin.indexer.repository.db.AllOutputsDbObject;
import com.bitcoin.indexer.repository.db.BlockDbObject;
import com.bitcoin.indexer.repository.db.TransactionDbObject;
import com.mongodb.MongoClientOptions;

import okhttp3.OkHttpClient;

@Configuration
public class Config implements InitializingBean {

	private static final Logger logger = LoggerFactory.getLogger(Config.class);

	@Autowired
	public ReactiveMongoTemplate reactiveMongoTemplate;

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

	@Value("${spring.data.mongodb.database}")
	String mongoDb;

	@Override
	public void afterPropertiesSet() throws Exception {
		logger.info("Running for database={}", mongoDb);
		ReactiveMongoTemplate mongoTemplate = reactiveMongoTemplate;
		mongoTemplate.indexOps(AllOutputsDbObject.class).ensureIndex(new Index("address", Direction.ASC).named("address_output")).block();
		mongoTemplate.indexOps(AllOutputsDbObject.class).ensureIndex(new Index("txId", Direction.ASC).named("txId_output")).block();

		Document document = new Document();
		document.put("address", 1);
		document.put("isSpent", 1);
		mongoTemplate.indexOps(AllOutputsDbObject.class).ensureIndex(new CompoundIndexDefinition(
				document
		).named("address_spent_output")).block();

		document = new Document();
		document.put("address", 1);
		document.put("isSpent", 1);
		document.put("slpUtxoType.parentTransactionValid.valid", 1);
		mongoTemplate.indexOps(AllOutputsDbObject.class).ensureIndex(new CompoundIndexDefinition(
				document
		).named("address_spent_valid_output")).block();

		document = new Document();
		document.put("slpUtxoType.slpTokenId", 1);
		document.put("slpUtxoType.hasBaton", 1);
		mongoTemplate.indexOps(AllOutputsDbObject.class).ensureIndex(new CompoundIndexDefinition(
				document
		).named("tokenId_baton_output")).block();

		document = new Document();
		document.put("slpUtxoType.slpTokenId", 1);
		document.put("slpUtxoType.amount", 1);
		document.put("isSpent", 1);
		mongoTemplate.indexOps(AllOutputsDbObject.class).ensureIndex(new CompoundIndexDefinition(
				document
		).named("tokenId_amount_spent_output")).block();

		document = new Document();
		document.put("slpUtxoType.slpTokenId", 1);
		document.put("isSpent", 1);
		document.put("slpUtxoType.parentTransactionValid.valid", 1);
		mongoTemplate.indexOps(AllOutputsDbObject.class).ensureIndex(new CompoundIndexDefinition(
				document
		).named("tokenId_spent_valid_output")).block();

		document = new Document();
		document.put("slpUtxoType.slpTokenId", 1);
		document.put("isSpent", 1);
		mongoTemplate.indexOps(AllOutputsDbObject.class).ensureIndex(new CompoundIndexDefinition(
				document
		).named("tokenId_spent_output")).block();

		document = new Document();
		document.put("_id", 1);
		document.put("slpValid.valid", 1);
		mongoTemplate.indexOps(TransactionDbObject.class).ensureIndex(new CompoundIndexDefinition(
				document
		).named("txId_valid_tx")).block();

		document = new Document();
		document.put("_id", 1);
		document.put("slpValid.valid", 1);
		document.put("outputs.slpUtxoType.slpTokenId", 1);
		mongoTemplate.indexOps(TransactionDbObject.class).ensureIndex(new CompoundIndexDefinition(
				document
		).named("txId_valid_tokenId_tx")).block();

		document = new Document();
		document.put("slpValid.valid", 1);
		document.put("outputs.slpUtxoType.slpTokenId", 1);
		mongoTemplate.indexOps(TransactionDbObject.class).ensureIndex(new CompoundIndexDefinition(
				document
		).named("valid_tokenId_tx")).block();

		mongoTemplate.indexOps(TransactionDbObject.class)
				.ensureIndex(new Index("outputs.slpUtxoType.slpTokenId", Direction.ASC).named("tokenId_tx")).block();

		mongoTemplate.indexOps(TransactionDbObject.class)
				.ensureIndex(new Index("time", Direction.DESC).named("time_tx")).block();

		mongoTemplate.indexOps(TransactionDbObject.class).ensureIndex(new Index(
				"inputs.address", Direction.ASC)
				.named("input_addr").background()).block();

		mongoTemplate.indexOps(TransactionDbObject.class).ensureIndex(new Index(
				"outputs.address", Direction.ASC)
				.named("output_addr").background()).block();

		mongoTemplate.indexOps(AllOutputsDbObject.class)
				.ensureIndex(new Index("slpUtxoType.slpTokenId", Direction.ASC).named("tokenId_output")).block();

		mongoTemplate.indexOps(BlockDbObject.class)
				.ensureIndex(new Index("height", Direction.ASC).named("block_height")).block();

	}
}
