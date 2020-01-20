package com.bitcoin.indexer.handlers;

import java.util.List;

import com.bitcoin.indexer.blockchain.domain.IndexerTransaction;
import com.bitcoin.indexer.blockchain.domain.Input;

import io.reactivex.Completable;
import io.reactivex.Single;

public interface InputHandler {

	Single<List<Input>> handleInput(IndexerTransaction transaction);

	Completable handleReorg(IndexerTransaction transactions);
}
