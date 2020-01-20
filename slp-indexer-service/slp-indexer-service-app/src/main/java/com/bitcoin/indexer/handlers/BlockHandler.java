package com.bitcoin.indexer.handlers;

import com.bitcoin.indexer.blockchain.domain.Block;

import io.reactivex.Completable;

public interface BlockHandler {

	Completable handleBlock(Block block);
}
