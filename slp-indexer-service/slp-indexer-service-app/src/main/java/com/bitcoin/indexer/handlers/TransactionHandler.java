package com.bitcoin.indexer.handlers;

import java.util.List;

import com.bitcoin.indexer.blockchain.domain.IndexerTransaction;

import io.reactivex.Completable;
import io.reactivex.Flowable;

public interface TransactionHandler {

	Flowable<IndexerTransaction> handleTransaction(List<IndexerTransaction> transaction);

	Completable reorganizeFromBlock(List<IndexerTransaction> oldTxs);

}
