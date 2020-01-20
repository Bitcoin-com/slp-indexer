package com.bitcoin.indexer.repository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.ReactiveMongoOperations;

import com.bitcoin.indexer.blockchain.domain.slp.SlpTokenId;
import com.bitcoin.indexer.blockchain.domain.slp.SlpVerifiedToken;

import io.reactivex.Maybe;

public class SlpVerifiedTokenRepositoryImpl implements SlpVerifiedTokenRepository {

	private static final Logger logger = LoggerFactory.getLogger(SlpVerifiedTokenRepositoryImpl.class);
	private ReactiveMongoOperations reactiveMongoOperations;

	public SlpVerifiedTokenRepositoryImpl(ReactiveMongoOperations reactiveMongoOperations) {
		this.reactiveMongoOperations = reactiveMongoOperations;
	}

	@Override
	public Maybe<SlpVerifiedToken> isVerified(SlpTokenId slpTokenId) {
		return Maybe.empty();
	}
}
