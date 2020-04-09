package com.bitcoin.indexer.repository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.BulkOperations.BulkMode;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.util.Pair;

import com.bitcoin.indexer.blockchain.domain.Address;
import com.bitcoin.indexer.blockchain.domain.IndexerTransaction;
import com.bitcoin.indexer.blockchain.domain.slp.SlpValid.Valid;
import com.bitcoin.indexer.repository.db.TransactionDbObject;
import com.bitcoin.indexer.core.Coin;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.collect.Lists;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import reactor.adapter.rxjava.RxJava2Adapter;

public class TransactionRepositoryImpl implements TransactionRepository {

	private MongoOperations mongoOperations;
	private ReactiveMongoTemplate reactiveMongoTemplate;
	private Coin coin;
	private ExecutorService executorService = Executors.newFixedThreadPool(10);
	private Cache<String, IndexerTransaction> transactionCache = Caffeine.newBuilder()
			.executor(Executors.newSingleThreadExecutor())
			.maximumSize(10000)
			.expireAfterWrite(1, TimeUnit.HOURS)
			.build();

	private static final Logger logger = LoggerFactory.getLogger(TransactionRepositoryImpl.class);

	public TransactionRepositoryImpl(MongoOperations mongoOperations, ReactiveMongoTemplate reactiveMongoTemplate,
			Coin coin) {
		this.mongoOperations = mongoOperations;
		this.reactiveMongoTemplate = reactiveMongoTemplate;
		this.coin = coin;
	}

	@Override
	public Maybe<IndexerTransaction> fetchTransaction(String txId, Coin coin, boolean useCache) {
		IndexerTransaction ifPresent = transactionCache.getIfPresent(txId);
		if (ifPresent != null) {
			return Maybe.just(ifPresent);
		}

		Query query = Query.query(Criteria.where("_id").is(txId));
		return RxJava2Adapter.monoToMaybe(reactiveMongoTemplate.findOne(query, TransactionDbObject.class))
				.map(e -> IndexerTransaction.create(e.toDomain()));
	}

	@Override
	public Single<List<IndexerTransaction>> fetchTransactions(List<String> txIds, Coin coin, boolean useCache) {
		Set<String> ids = new HashSet<>(txIds);
		Set<IndexerTransaction> result = new HashSet<>();

		for (String txId : txIds) {
			IndexerTransaction ifPresent = transactionCache.getIfPresent(txId);
			if (ifPresent != null) {
				result.add(ifPresent);
				ids.remove(txId);
			}
		}

		Query query = Query.query(Criteria.where("_id").in(ids));
		return RxJava2Adapter.fluxToFlowable(reactiveMongoTemplate.find(query, TransactionDbObject.class))
				.map(e -> IndexerTransaction.create(e.toDomain()))
				.mergeWith(Flowable.fromIterable(result))
				.toList();
	}

	@Override
	public Single<List<IndexerTransaction>> fetchTransactionsInvolvingToken(String tokenId, boolean useCache, int page) {
		int limit = 10;
		int skip = limit * (page - 1);
		Query query = Query.query(Criteria.where("slpValid.valid").is(Valid.VALID.name())
				.and("outputs.slpUtxoType.slpTokenId").is(tokenId)).limit(limit).skip(skip).with(Sort.by(Direction.DESC, "time"));

		return RxJava2Adapter.fluxToFlowable(reactiveMongoTemplate.find(query, TransactionDbObject.class))
				.map(TransactionDbObject::toDomain)
				.map(IndexerTransaction::create)
				.toList();
	}

	@Override
	public Single<List<String>> fetchTransactionIdsByAddress(String address, Coin coin) {
		return null;
	}

	@Override
	public Single<List<IndexerTransaction>> fetchTransactions(Address address, Coin coin) {
		Query first = Query.query(Criteria.where("outputs.address").is(address.getAddress()));
		Query second = Query.query(Criteria.where("inputs.address").is(address.getAddress()));
		return RxJava2Adapter.fluxToFlowable(reactiveMongoTemplate.find(first, TransactionDbObject.class))
				.mergeWith(RxJava2Adapter.fluxToFlowable(reactiveMongoTemplate.find(second, TransactionDbObject.class)))
				.map(TransactionDbObject::toDomain)
				.map(IndexerTransaction::create)
				.toList()
				.doOnError(er -> logger.error("Could not fetch txs for address={}", address));
	}

	@Override
	public Single<List<IndexerTransaction>> fetchValidTransactions(List<String> txIds, Coin coin, boolean useCache) {
		Set<String> ids = new HashSet<>(txIds);
		Set<IndexerTransaction> result = new HashSet<>();

		for (String txId : txIds) {
			IndexerTransaction ifPresent = transactionCache.getIfPresent(txId);
			if (ifPresent != null) {
				result.add(ifPresent);
				ids.remove(txId);
			}
		}
		List<List<String>> partition = Lists.partition(txIds, 10000);
		List<Flowable<IndexerTransaction>> queryFlow = new ArrayList<>();
		logger.info("Starting query size={} partitions={}", txIds.size(), partition.size());
		for (List<String> listOfTxIds : partition) {
			Criteria criteria = new Criteria();
			criteria.andOperator(Criteria.where("_id").in(listOfTxIds), Criteria.where("slpValid.valid").is(Valid.VALID.name()));
			Query query = Query.query(criteria);
			Flowable<IndexerTransaction> flow = RxJava2Adapter.fluxToFlowable(reactiveMongoTemplate.find(query, TransactionDbObject.class))
					.map(e -> IndexerTransaction.create(e.toDomain()))
					.mergeWith(Flowable.fromIterable(result));
			queryFlow.add(flow);
		}
		logger.info("Query setup done");
		return Flowable.mergeDelayError(queryFlow, 4)
				.toList()
				.doOnSuccess(txs -> {
					logger.info("Query done found={}", txs.size());
					executorService.submit(() -> txs.forEach(t -> transactionCache.put(t.getTransaction().getTxId(), t)));
				});
	}

	@Override
	public Single<BigDecimal> transactionsForTokenId(String tokenId) {
		Query query = Query.query(Criteria.where("outputs.slpUtxoType.slpTokenId").is(tokenId));
		return RxJava2Adapter.monoToSingle(reactiveMongoTemplate.count(query, "transactions"))
				.map(BigDecimal::new);
	}

	@Override
	public Single<Map<String, BigDecimal>> transactionsForTokenIds(List<String> tokenIds) {
		return Flowable.fromIterable(tokenIds).flatMapSingle(tokenId -> transactionsForTokenId(tokenId)
				.map(res -> Pair.of(tokenId, res)))
				.groupBy(Pair::getFirst)
				.flatMapSingle(group -> group.toMap(Pair::getFirst, Pair::getSecond))
				.reduce((result, result2) -> {
					result.putAll(result2);
					return result;
				})
				.toSingle(Map.of());
	}

	@Override
	public Single<List<IndexerTransaction>> saveTransaction(List<IndexerTransaction> indexerTransaction) {
		BulkOperations bulkOperations = mongoOperations.bulkOps(BulkMode.UNORDERED, TransactionDbObject.class);
		for (IndexerTransaction transaction : indexerTransaction) {
			TransactionDbObject transactionDbObject = TransactionDbObject.fromDomain(transaction.getTransaction(), coin);
			bulkOperations.upsert(Query.query(Criteria.where("_id").is(transaction.getTransaction().getTxId())), transactionDbObject.toUpdate());
		}

		executorService.submit(() -> {
			indexerTransaction.forEach(s -> transactionCache.put(s.getTransaction().getTxId(), s));
		});

		bulkOperations.execute();
		return Single.just(indexerTransaction);
	}

	@Override
	public Completable handleReorg(List<IndexerTransaction> oldTxs) {
		BulkOperations bulkOperations = mongoOperations.bulkOps(BulkMode.UNORDERED, TransactionDbObject.class);
		for (IndexerTransaction transaction : oldTxs) {
			bulkOperations.remove(Query.query(Criteria.where("_id").is(transaction.getTransaction().getTxId())));
		}

		executorService.submit(() -> {
			oldTxs.forEach(s -> transactionCache.invalidate(s.getTransaction().getTxId()));
		});

		bulkOperations.execute();
		return Completable.complete();
	}
}
