package com.bitcoin.indexer.repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.BulkOperations.BulkMode;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.ReactiveMongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.util.Pair;

import com.bitcoin.indexer.blockchain.domain.Address;
import com.bitcoin.indexer.blockchain.domain.Input;
import com.bitcoin.indexer.blockchain.domain.Transaction;
import com.bitcoin.indexer.blockchain.domain.Utxo;
import com.bitcoin.indexer.blockchain.domain.UtxoMinimalData;
import com.bitcoin.indexer.blockchain.domain.slp.SlpValid;
import com.bitcoin.indexer.blockchain.domain.slp.SlpValid.Valid;
import com.bitcoin.indexer.blockchain.domain.timers.SystemTimer;
import com.bitcoin.indexer.repository.db.AllOutputsDbObject;
import com.bitcoin.indexer.core.Coin;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.bulk.BulkWriteUpsert;

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
	public Single<List<Utxo>> fetchUtxosFromAddress(Address address, Coin coin, boolean useCache, Valid parentValidation) {
		Query query = Query.query(Criteria.where("address").is(address.getAddress()))
				.addCriteria(Criteria.where("isSpent").is(false))
				.addCriteria(Criteria.where("slpUtxoType.parentTransactionValid.valid").is(parentValidation.name()));
		return RxJava2Adapter.fluxToFlowable(reactiveMongoOperations.find(query, AllOutputsDbObject.class)
				.doOnError(er -> logger.error("Could not fetch address={} slputxos={}", address, er)))
				.map(AllOutputsDbObject::toDomain)
				.toList();
	}

	@Override
	public Single<List<Utxo>> fetchSlpUtxosForAddress(Address address, Coin coin, boolean useCache, Valid parentValidation) {
		/*List<Utxo> ifPresent = utxosCache.getIfPresent(address.getAddress());
		if (ifPresent != null) {
			return Single.just(ifPresent);
		}*/

		return fetchUtxosFromAddress(address, coin, useCache, parentValidation)
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
	public Single<List<Utxo>> fetchUtxosWithTokenId(List<String> tokenIds, boolean isSpent, Valid parentValidation) {
		SystemTimer systemTimer = SystemTimer.create();
		systemTimer.start();
		Document hint = new Document();
		hint.put("slpUtxoType.slpTokenId", 1);
		hint.put("isSpent", 1);
		hint.put("slpUtxoType.parentTransactionValid.valid", 1);
		Query query = Query.query(Criteria.where("slpUtxoType.slpTokenId").in(tokenIds).and("isSpent").is(isSpent))
				.addCriteria(Criteria.where("slpUtxoType.parentTransactionValid.valid").is(parentValidation.name()))
				.withHint(hint);

		return RxJava2Adapter.fluxToFlowable(reactiveMongoOperations.find(query, AllOutputsDbObject.class))
				.map(AllOutputsDbObject::toDomain)
				.toList()
				.doOnSuccess(s -> {
					logger.trace("Completed utxos={} fetchUtxosWithTokenId={}", s.size(), systemTimer.getMsSinceStart());
				});
	}

	@Override
	public Single<List<UtxoMinimalData>> fetchMinimalUtxoData(List<String> tokenIds, boolean isSpent, Valid parentValidation) {
		SystemTimer systemTimer = SystemTimer.create();
		systemTimer.start();
		Document hint = new Document();
		hint.put("slpUtxoType.slpTokenId", 1);
		hint.put("isSpent", 1);
		hint.put("slpUtxoType.parentTransactionValid.valid", 1);
		Query query = Query.query(Criteria.where("slpUtxoType.slpTokenId").in(tokenIds).and("isSpent").is(isSpent))
				.addCriteria(Criteria.where("slpUtxoType.parentTransactionValid.valid").is(parentValidation.name()))
				.withHint(hint);
		query.fields()
				.include("slpUtxoType.slpTokenId")
				.include("slpUtxoType.amount")
				.include("slpUtxoType.hasBaton")
				.include("address")
				.include("value")
				.include("slpUtxoType.tokenTransactionType");

		return RxJava2Adapter.fluxToFlowable(reactiveMongoOperations.find(query, AllOutputsDbObject.class))
				.map(result -> new UtxoMinimalData(result.getId().split(":")[0],
						new BigDecimal(result.getSlpUtxoType().getAmount()),
						result.getSlpUtxoType().isHasBaton(), Address.create(result.getAddress()),
						result.getValue(),
						result.getSlpUtxoType().getTokenTransactionType(),
						result.getSlpUtxoType().getSlpTokenId()))
				.toList()
				.doOnSuccess(s -> logger.trace("Completed utxos={} fetchUtxosWithTokenId={}", s.size(), systemTimer.getMsSinceStart()));
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

	@Override
	public Single<List<Transaction>> updateUtxoValidationStatus(List<Transaction> txs, Coin coin) {
		if (txs.isEmpty()) {
			return Single.just(txs);
		}
		List<Utxo> allUtxos = txs.stream().map(Transaction::getOutputs)
				.flatMap(Collection::stream)
				.filter(e -> e.getSlpUtxo().isPresent())
				.collect(Collectors.toList());

		if (allUtxos.isEmpty()) {
			return Single.just(txs);
		}

		Map<String, SlpValid> txIdValidation = txs.stream()
				.filter(e -> e.getSlpValid().isPresent())
				.collect(Collectors.toMap(Transaction::getTxId, v -> v.getSlpValid().get()));

		BulkOperations bulkOperations = mongoOperations.bulkOps(BulkMode.UNORDERED, AllOutputsDbObject.class);

		for (Utxo bulk : allUtxos) {
			Update update = new Update();
			Document document = new Document();
			SlpValid slpValid = txIdValidation.get(bulk.getTxId());
			document.put("reason", slpValid.getReason());
			document.put("valid", slpValid.getValid().name());
			update.set("slpUtxoType.parentTransactionValid", document);
			String key = AllOutputsDbObject.keyParser(bulk.getTxId(), bulk.getIndex());
			logger.trace("Adding utxo valid status key={} txId={} index={} valid={} update={}", key, bulk.getTxId(), bulk.getIndex(), slpValid.getValid(), update.toString());
			bulkOperations.upsert(Query.query(Criteria.where("_id").is(key)), update);
			refreshCache(bulk);
		}
		BulkWriteResult execute = bulkOperations.execute();
		logger.trace("utxos={} inserted={} modified={} matched={} upserts={}", allUtxos.size(),
				execute.getInsertedCount(),
				execute.getModifiedCount(),
				execute.getMatchedCount(),
				execute.getUpserts().stream().map(BulkWriteUpsert::getId).collect(Collectors.toList()));
		return Single.just(txs);
	}

	public void addToCache(Utxo utxo) {
		txIdIndexCache.put(AllOutputsDbObject.keyParser(utxo.getTxId(), utxo.getIndex()), utxo);
	}

	public void refreshCache(Utxo utxo) {
		String key = AllOutputsDbObject.keyParser(utxo.getTxId(), utxo.getIndex());
		Utxo ifPresent = txIdIndexCache.getIfPresent(key);
		if (ifPresent == null) {
			return;
		}
		if (utxo.getSlpUtxo().isEmpty()) {
			return;
		}
		Utxo withValid = ifPresent.withValid(utxo.getSlpUtxo().get().getParentTransactionValid());
		txIdIndexCache.put(key, withValid);
	}
}
