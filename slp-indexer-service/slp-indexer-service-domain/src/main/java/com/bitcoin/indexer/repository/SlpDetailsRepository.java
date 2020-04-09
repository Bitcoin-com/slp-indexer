package com.bitcoin.indexer.repository;

import java.util.List;

import com.bitcoin.indexer.blockchain.domain.slp.ExtendedDetails;
import com.bitcoin.indexer.blockchain.domain.slp.SlpTokenDetails;
import com.bitcoin.indexer.blockchain.domain.slp.SlpTokenId;

import io.reactivex.Maybe;
import io.reactivex.Single;

public interface SlpDetailsRepository {

	Maybe<SlpTokenDetails> fetchSlpDetails(SlpTokenId slpTokenId);

	Single<List<SlpTokenDetails>> fetchSlpDetails(List<SlpTokenId> slpTokenIds);

	Single<SlpTokenDetails> saveSlpTokenDetails(SlpTokenDetails slpTokenDetails, Integer lastActiveMint, Integer lastActiveSend, Integer blockCreated);

	Maybe<SlpTokenDetails> updateMetadata(SlpTokenId slpTokenId, Integer lastActiveMint, Integer lastActiveSend);

	Single<List<ExtendedDetails>> fetchExtendedDetails(List<SlpTokenId> slpTokenIds);
}
