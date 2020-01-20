package com.bitcoin.indexer.handlers;

import java.util.List;

import com.bitcoin.indexer.blockchain.domain.Utxo;
import com.bitcoin.indexer.blockchain.domain.IndexerTransaction;

import io.reactivex.Completable;
import io.reactivex.Single;

public interface UtxoHandler {

	Single<List<Utxo>> handleUtxos(IndexerTransaction indexerTransaction);

	Completable handleReorg(IndexerTransaction indexerTransaction);
}
