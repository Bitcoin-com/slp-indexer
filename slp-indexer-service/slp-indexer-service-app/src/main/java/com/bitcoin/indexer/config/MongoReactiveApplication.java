package com.bitcoin.indexer.config;

import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.index.CompoundIndexDefinition;
import org.springframework.data.mongodb.core.index.Index;

import com.bitcoin.indexer.repository.db.AllOutputsDbObject;
import com.bitcoin.indexer.repository.db.BlockDbObject;
import com.bitcoin.indexer.repository.db.TransactionDbObject;

@Configuration
public class MongoReactiveApplication implements InitializingBean, DisposableBean {

	private static final Logger logger = LoggerFactory.getLogger(MongoReactiveApplication.class);

	@Autowired
	ReactiveMongoTemplate reactiveMongoTemplate;

	@Autowired
	MongoDbFactory mongoDbFactory;

	@Bean
	public MongoTemplate mongoTemplate() {
		return new MongoTemplate(mongoDbFactory);
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		MongoTemplate mongoTemplate = mongoTemplate();
		mongoTemplate.indexOps(AllOutputsDbObject.class).ensureIndex(new Index("address", Direction.ASC).named("address_output"));
		mongoTemplate.indexOps(AllOutputsDbObject.class).ensureIndex(new Index("txId", Direction.ASC).named("txId_output"));

		Document document = new Document();
		document.put("address", 1);
		document.put("isSpent", 1);
		mongoTemplate.indexOps(AllOutputsDbObject.class).ensureIndex(new CompoundIndexDefinition(
				document
		).named("address_spent_output"));

		document = new Document();
		document.put("address", 1);
		document.put("isSpent", 1);
		document.put("slpUtxoType.parentTransactionValid.valid", 1);
		mongoTemplate.indexOps(AllOutputsDbObject.class).ensureIndex(new CompoundIndexDefinition(
				document
		).named("address_spent_valid_output"));

		document = new Document();
		document.put("slpUtxoType.slpTokenId", 1);
		document.put("slpUtxoType.hasBaton", 1);
		mongoTemplate.indexOps(AllOutputsDbObject.class).ensureIndex(new CompoundIndexDefinition(
				document
		).named("tokenId_baton_output"));

		document = new Document();
		document.put("slpUtxoType.slpTokenId", 1);
		document.put("slpUtxoType.amount", 1);
		document.put("isSpent", 1);
		mongoTemplate.indexOps(AllOutputsDbObject.class).ensureIndex(new CompoundIndexDefinition(
				document
		).named("tokenId_amount_spent_output"));

		document = new Document();
		document.put("slpUtxoType.slpTokenId", 1);
		document.put("isSpent", 1);
		document.put("slpUtxoType.parentTransactionValid.valid", 1);
		mongoTemplate.indexOps(AllOutputsDbObject.class).ensureIndex(new CompoundIndexDefinition(
				document
		).named("tokenId_spent_valid_output"));

		document = new Document();
		document.put("slpUtxoType.slpTokenId", 1);
		document.put("isSpent", 1);
		mongoTemplate.indexOps(AllOutputsDbObject.class).ensureIndex(new CompoundIndexDefinition(
				document
		).named("tokenId_spent_output"));

		document = new Document();
		document.put("_id", 1);
		document.put("slpValid.valid", 1);
		mongoTemplate.indexOps(TransactionDbObject.class).ensureIndex(new CompoundIndexDefinition(
				document
		).named("txId_valid_tx"));

		document = new Document();
		document.put("_id", 1);
		document.put("slpValid.valid", 1);
		document.put("outputs.slpUtxoType.slpTokenId", 1);
		mongoTemplate.indexOps(TransactionDbObject.class).ensureIndex(new CompoundIndexDefinition(
				document
		).named("txId_valid_tokenId_tx"));

		document = new Document();
		document.put("slpValid.valid", 1);
		document.put("outputs.slpUtxoType.slpTokenId", 1);
		mongoTemplate.indexOps(TransactionDbObject.class).ensureIndex(new CompoundIndexDefinition(
				document
		).named("valid_tokenId_tx"));

		mongoTemplate.indexOps(TransactionDbObject.class)
				.ensureIndex(new Index("outputs.slpUtxoType.slpTokenId", Direction.ASC).named("tokenId_tx"));

		mongoTemplate.indexOps(TransactionDbObject.class)
				.ensureIndex(new Index("time", Direction.DESC).named("time_tx"));

		mongoTemplate.indexOps(AllOutputsDbObject.class)
				.ensureIndex(new Index("slpUtxoType.slpTokenId", Direction.ASC).named("tokenId_output"));

		mongoTemplate.indexOps(BlockDbObject.class)
				.ensureIndex(new Index("height", Direction.ASC).named("block_height"));
	}

	@Override
	public void destroy() throws Exception {

	}
}