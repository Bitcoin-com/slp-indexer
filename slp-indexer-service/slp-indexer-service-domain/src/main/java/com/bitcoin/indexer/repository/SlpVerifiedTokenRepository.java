package com.bitcoin.indexer.repository;

import com.bitcoin.indexer.blockchain.domain.slp.SlpTokenId;
import com.bitcoin.indexer.blockchain.domain.slp.SlpVerifiedToken;
import io.reactivex.Maybe;

public interface SlpVerifiedTokenRepository {

	Maybe<SlpVerifiedToken> isVerified(SlpTokenId slpTokenId);
}
