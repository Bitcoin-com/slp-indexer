package com.bitcoin.indexer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.bitcoin.indexer.blockchain.domain.slp.ExtendedDetails;
import com.bitcoin.indexer.blockchain.domain.slp.SlpTokenDetails;
import com.bitcoin.indexer.blockchain.domain.slp.SlpTokenId;
import com.bitcoin.indexer.repository.SlpDetailsRepository;

import io.reactivex.Maybe;
import io.reactivex.Single;

public class FakeInMemoryDetails implements SlpDetailsRepository {
	private Map<String, SlpTokenDetails> detailsMap = new HashMap<>();

	@Override
	public Maybe<SlpTokenDetails> fetchSlpDetails(SlpTokenId slpTokenId) {
		SlpTokenDetails slpTokenDetails = detailsMap.get(slpTokenId.getHex());
		if (slpTokenDetails == null) {
			return Maybe.empty();
		}

		return Maybe.just(slpTokenDetails);
	}

	@Override
	public Single<List<SlpTokenDetails>> fetchSlpDetails(List<SlpTokenId> slpTokenIds) {
		return Single.just(new ArrayList<>(detailsMap.values()));
	}

	@Override
	public Single<SlpTokenDetails> saveSlpTokenDetails(SlpTokenDetails slpTokenDetails, Integer lastActiveMint, Integer lastActiveSend, Integer blockCreated) {
		detailsMap.put(slpTokenDetails.getTokenId().getHex(), slpTokenDetails);
		return Single.just(slpTokenDetails);
	}

	@Override
	public Maybe<SlpTokenDetails> updateMetadata(SlpTokenId slpTokenId, Integer lastActiveMint, Integer lastActiveSend) {
		return fetchSlpDetails(slpTokenId);
	}

	@Override
	public Single<List<ExtendedDetails>> fetchExtendedDetails(List<SlpTokenId> slpTokenIds) {
		return Single.just(List.of());
	}
}
