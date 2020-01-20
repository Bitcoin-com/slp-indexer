package com.bitcoin.indexer.repository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.ReactiveMongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import com.bitcoin.indexer.blockchain.domain.Block;
import com.bitcoin.indexer.repository.db.BlockDbObject;
import com.bitcoin.indexer.repository.db.HeightDbObject;

import io.reactivex.Maybe;
import io.reactivex.Single;
import reactor.adapter.rxjava.RxJava2Adapter;

public class BlockRepositoryImpl implements BlockRepository {

	private ReactiveMongoOperations reactiveMongoOperations;
	private static final Logger logger = LoggerFactory.getLogger(BlockRepositoryImpl.class);

	public BlockRepositoryImpl(ReactiveMongoOperations reactiveMongoOperations) {
		this.reactiveMongoOperations = reactiveMongoOperations;
	}

	@Override
	public Single<Block> saveBlock(Block block) {
		BlockDbObject blockDbObject = BlockDbObject.fromDomain(block);

		Query id = Query.query(Criteria.where("_id").is(block.getHash()));
		return RxJava2Adapter.monoToMaybe(reactiveMongoOperations.upsert(id, blockDbObject.toUpdate(), BlockDbObject.class)
				.doOnError(er -> logger.error("Error saving block hash={}", block.getHash(), er)))
				.map(e -> block)
				.defaultIfEmpty(block)
				.toSingle();
	}

	@Override
	public Single<Long> saveHeight(Long currentHeight) {
		HeightDbObject heightDbObject = new HeightDbObject(currentHeight);
		Query current_height = Query.query(Criteria.where("_id").is("CURRENT_HEIGHT"));

		Update current_height1 = Update.update("height", heightDbObject.getHeight());

		return RxJava2Adapter.monoToMaybe(reactiveMongoOperations.upsert(current_height, current_height1, HeightDbObject.class)
				.doOnError(er -> logger.error("Error saving current height={}", currentHeight, er)))
				.map(e -> currentHeight)
				.toSingle(currentHeight);
	}

	@Override
	public Single<Long> currentHeight() {
		return null;
	}

	@Override
	public Maybe<Block> getBlock(String hash) {
		Query query = Query.query(Criteria.where("_id").is(hash));
		return RxJava2Adapter.monoToMaybe(reactiveMongoOperations.findOne(query, BlockDbObject.class))
				.map(BlockDbObject::toDomain);
	}
}
