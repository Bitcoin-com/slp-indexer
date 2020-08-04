package com.bitcoin.indexer.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import com.bitcoin.indexer.blockchain.domain.Address;
import com.bitcoin.indexer.blockchain.domain.IndexerTransaction;
import com.bitcoin.indexer.core.Coin;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Single;

public interface TransactionRepository {

	Maybe<IndexerTransaction> fetchTransaction(String txId, Coin coin, boolean useCache);

	Single<List<IndexerTransaction>> fetchTransactions(List<String> txIds, Coin coin, boolean useCache);

	Single<List<IndexerTransaction>> fetchTransactionsInvolvingToken(String tokenId, boolean useCache, int page);

	Single<List<String>> fetchTransactionIdsByAddress(String address, Coin coin);

	Single<List<IndexerTransaction>> fetchTransactions(Address address, Coin coin);

	Single<List<IndexerTransaction>> fetchValidTransactions(List<String> txIds, Coin coin, boolean useCache);

	Single<BigDecimal> transactionsForTokenId(String tokenId);

	Single<Map<String, BigDecimal>> transactionsForTokenIds(List<String> tokenIds);

	Flowable<IndexerTransaction> fetchTransactions(Integer height, String tokenId, int page, Coin coin);

	//Internal use api
	Single<List<IndexerTransaction>> saveTransaction(List<IndexerTransaction> indexerTransaction);


	Completable handleReorg(List<IndexerTransaction> oldTxs);

}
