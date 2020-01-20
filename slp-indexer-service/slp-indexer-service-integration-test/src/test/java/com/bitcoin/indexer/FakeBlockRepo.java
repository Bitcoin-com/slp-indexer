package com.bitcoin.indexer;

import com.bitcoin.indexer.blockchain.domain.Block;
import com.bitcoin.indexer.repository.BlockRepository;

import io.reactivex.Maybe;
import io.reactivex.Single;

public class FakeBlockRepo implements BlockRepository {
	@Override
	public Single<Block> saveBlock(Block block) {
		return Single.just(block);
	}

	@Override
	public Single<Long> saveHeight(Long currentHeight) {
		return null;
	}

	@Override
	public Single<Long> currentHeight() {
		return null;
	}

	@Override
	public Maybe<Block> getBlock(String hash) {
		return Maybe.empty();
	}
}
