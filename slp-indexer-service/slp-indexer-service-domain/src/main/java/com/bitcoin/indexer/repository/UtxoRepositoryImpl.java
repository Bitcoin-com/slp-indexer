package com.bitcoin.indexer.repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.BulkOperations.BulkMode;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.ReactiveMongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.util.Pair;

import com.bitcoin.indexer.blockchain.domain.Address;
import com.bitcoin.indexer.blockchain.domain.Input;
import com.bitcoin.indexer.blockchain.domain.Utxo;
import com.bitcoin.indexer.core.Coin;
import com.bitcoin.indexer.blockchain.domain.timers.SystemTimer;
import com.bitcoin.indexer.repository.db.AllOutputsDbObject;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import reactor.adapter.rxjava.RxJava2Adapter;

public class UtxoRepositoryImpl implements UtxoRepository {

	private boolean initialSync;
	private ReactiveMongoOperations reactiveMongoOperations;
	private MongoOperations mongoOperations;
	private static final Logger logger = LoggerFactory.getLogger(UtxoRepositoryImpl.class);
	private final Cache<String, Utxo> txIdIndexCache;
	//private final Cache<String, List<Utxo>> utxosCache;

	public UtxoRepositoryImpl(boolean initialSync,
			ReactiveMongoOperations reactiveMongoOperations,
			MongoOperations mongoOperations, int cacheSize) {
		this.initialSync = initialSync;
		this.reactiveMongoOperations = reactiveMongoOperations;
		this.mongoOperations = mongoOperations;
		txIdIndexCache = Caffeine.newBuilder()
				.maximumSize(cacheSize)
				.expireAfterWrite(40, TimeUnit.MINUTES)
				.executor(Executors.newSingleThreadExecutor())
				.build();

		/*utxosCache = Caffeine.newBuilder()
				.maximumSize(cacheSize)
				.expireAfterWrite(5, TimeUnit.SECONDS)
				.executor(Executors.newSingleThreadExecutor())
				.build();*/
	}

	@Override
	public Single<List<Utxo>> fetchUtxosFromAddress(Address address, Coin coin, boolean useCache) {
		Query query = Query.query(Criteria.where("address").is(address.getAddress()))
				.addCriteria(Criteria.where("isSpent").is(false));
		return RxJava2Adapter.fluxToFlowable(reactiveMongoOperations.find(query, AllOutputsDbObject.class)
				.doOnError(er -> logger.error("Could not fetch address={} slputxos={}", address, er)))
				.map(AllOutputsDbObject::toDomain)
				.toList();
	}

	@Override
	public Single<List<Utxo>> fetchSlpUtxosForAddress(Address address, Coin coin, boolean useCache) {
		/*List<Utxo> ifPresent = utxosCache.getIfPresent(address.getAddress());
		if (ifPresent != null) {
			return Single.just(ifPresent);
		}*/

		return fetchUtxosFromAddress(address, coin, useCache)
				.toFlowable()
				.flatMap(Flowable::fromIterable)
				.filter(utxo -> utxo.getSlpUtxo().isPresent())
				.toList();
		//.doOnSuccess(s -> utxosCache.put(address.getAddress(), s));
	}

	@Override
	public Maybe<Utxo> fetchUtxo(String txId, int inputIndex, Coin coin) {
		Utxo ifPresent = txIdIndexCache.getIfPresent(AllOutputsDbObject.keyParser(txId, inputIndex));
		if (ifPresent != null) {
			return Maybe.just(ifPresent);
		}

		Query query = Query.query(Criteria.where("_id").is(AllOutputsDbObject.keyParser(txId, inputIndex)));
		return RxJava2Adapter.monoToMaybe(reactiveMongoOperations.findOne(query, AllOutputsDbObject.class)
				.doOnError(er -> logger.error("Error fetching utxo txId={} index={}", txId, inputIndex, er)))
				.map(AllOutputsDbObject::toDomain);
	}

	@Override
	public Single<List<Utxo>> fetchUtxosWithTokenId(List<String> tokenIds, boolean isSpent) {
		Query query = Query.query(Criteria.where("slpUtxoType.slpTokenId").in(tokenIds).and("isSpent").is(isSpent));
		return RxJava2Adapter.fluxToFlowable(reactiveMongoOperations.find(query, AllOutputsDbObject.class))
				.map(AllOutputsDbObject::toDomain)
				.toList();
	}

	@Override
	public Single<List<Utxo>> fetchUtxos(List<Pair<String, Integer>> txIdIndexs, Coin coin) {
		List<Utxo> result = new ArrayList<>();
		List<String> queries = new ArrayList<>();
		for (Pair<String, Integer> pair : txIdIndexs) {
			Utxo ifPresent = txIdIndexCache.getIfPresent(AllOutputsDbObject.keyParser(pair.getFirst(), pair.getSecond()));
			if (ifPresent != null) {
				result.add(ifPresent);
			} else {
				queries.add(AllOutputsDbObject.keyParser(pair.getFirst(), pair.getSecond()));
			}
		}
		return RxJava2Adapter.fluxToFlowable(reactiveMongoOperations.find(Query.query(Criteria.where("_id").in(queries)), AllOutputsDbObject.class))
				.map(AllOutputsDbObject::toDomain)
				.mergeWith(Flowable.fromIterable(result))
				.toList();

	}

	@Override
	public Single<List<Utxo>> fetchUtxo(List<Input> inputs, Coin coin) {
		List<Utxo> result = new ArrayList<>();
		List<String> queries = new ArrayList<>();
		for (Input input : inputs) {
			Utxo ifPresent = txIdIndexCache.getIfPresent(AllOutputsDbObject.keyParser(input.getTxId(), input.getIndex()));
			if (ifPresent != null) {
				result.add(ifPresent);
			} else {
				queries.add(AllOutputsDbObject.keyParser(input.getTxId(), input.getIndex()));
			}
		}
		return RxJava2Adapter.fluxToFlowable(reactiveMongoOperations.find(Query.query(Criteria.where("_id").in(queries)), AllOutputsDbObject.class))
				.map(AllOutputsDbObject::toDomain)
				.mergeWith(Flowable.fromIterable(result))
				.doOnNext(s -> txIdIndexCache.put(s.getTxId(), s))
				.toList();
	}

	@Override
	public Single<List<Utxo>> fetchUtxos(String txId, Coin coin) {
		Query query = Query.query(Criteria.where("txId").is(txId));

		return RxJava2Adapter.fluxToFlowable(reactiveMongoOperations.find(query, AllOutputsDbObject.class))
				.map(AllOutputsDbObject::toDomain)
				.toList();
	}

	@Override
	public Single<List<Utxo>> saveUtxo(List<Utxo> utxo, Coin coin) {
		return Flowable.fromIterable(utxo)
				.map(u -> {
					Utxo old = fetchUtxo(u.getTxId(), u.getIndex(), coin).blockingGet();
					if (old == null) {
						txIdIndexCache.put(AllOutputsDbObject.keyParser(u.getTxId(), u.getIndex()), u);
						return u;
					}
					Utxo withOldTimestamp = u.withTimestamp(old.getTimestamp());
					txIdIndexCache.put(AllOutputsDbObject.keyParser(u.getTxId(), u.getIndex()), withOldTimestamp);
					return withOldTimestamp;
				})
				.toList()
				.flatMap(u -> insertAllUtxo(u, coin));
	}

	private Single<List<Utxo>> insertAllUtxo(List<Utxo> utxo, Coin coin) {
		if (utxo.isEmpty()) {
			return Single.just(utxo);
		}

		BulkOperations bulkOperations = mongoOperations.bulkOps(BulkMode.UNORDERED, AllOutputsDbObject.class);
		for (Utxo bulk : utxo) {
			AllOutputsDbObject allOutputsDbObject = AllOutputsDbObject.fromDomain(bulk, null, false, coin);
			bulkOperations.upsert(Query.query(Criteria.where("_id").is(AllOutputsDbObject.keyParser(bulk.getTxId(), bulk.getIndex()))), allOutputsDbObject.toUpdate());
		}
		bulkOperations.execute();
		return Single.just(utxo);
	}

	@Override
	public Single<List<Utxo>> spendUtxo(List<Utxo> utxo, Coin coin) {
		if (utxo.isEmpty()) {
			return Single.just(utxo);
		}

		BulkOperations bulkOperations = mongoOperations.bulkOps(BulkMode.UNORDERED, AllOutputsDbObject.class);
		for (Utxo bulk : utxo) {
			txIdIndexCache.invalidate(AllOutputsDbObject.keyParser(bulk.getTxId(), bulk.getIndex()));
			AllOutputsDbObject allOutputsDbObject = AllOutputsDbObject.fromDomain(bulk, Instant.now(), true, coin);
			bulkOperations.upsert(Query.query(Criteria.where("_id").is(AllOutputsDbObject.keyParser(bulk.getTxId(), bulk.getIndex()))), allOutputsDbObject.toUpdate());
		}
		bulkOperations.execute();

		return Single.just(utxo);
	}

	@Override
	public Single<List<Utxo>> removeUtxo(List<Utxo> utxo, Coin coin) {
		BulkOperations bulkOperations = mongoOperations.bulkOps(BulkMode.UNORDERED, AllOutputsDbObject.class);
		List<Query> deletes = new ArrayList<>();
		for (Utxo bulk : utxo) {
			deletes.add(Query.query(Criteria.where("_id").is(AllOutputsDbObject.keyParser(bulk.getTxId(), bulk.getIndex()))));
		}
		bulkOperations.remove(deletes);
		bulkOperations.execute();
		return Single.just(utxo);
	}

	public void addToCache(Utxo utxo) {
		txIdIndexCache.put(AllOutputsDbObject.keyParser(utxo.getTxId(), utxo.getIndex()), utxo);
	}
}
