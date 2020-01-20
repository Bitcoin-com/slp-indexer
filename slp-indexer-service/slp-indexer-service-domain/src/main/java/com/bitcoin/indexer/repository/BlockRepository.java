package com.bitcoin.indexer.repository;

import com.bitcoin.indexer.blockchain.domain.Block;

import io.reactivex.Maybe;
import io.reactivex.Single;

public interface BlockRepository {

	Single<Block> saveBlock(Block block);

	Single<Long> saveHeight(Long currentHeight);

	Single<Long> currentHeight();

	Maybe<Block> getBlock(String hash);

}
