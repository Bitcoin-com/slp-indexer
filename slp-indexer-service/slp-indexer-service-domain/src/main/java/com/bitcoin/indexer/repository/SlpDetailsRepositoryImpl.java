package com.bitcoin.indexer.repository;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.ReactiveMongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import com.bitcoin.indexer.blockchain.domain.slp.SlpTokenDetails;
import com.bitcoin.indexer.blockchain.domain.slp.SlpTokenId;
import com.bitcoin.indexer.repository.db.SlpTokenDetailsDbObject;

import io.reactivex.Maybe;
import io.reactivex.Single;
import reactor.adapter.rxjava.RxJava2Adapter;

public class SlpDetailsRepositoryImpl implements SlpDetailsRepository {

	private static final Logger logger = LoggerFactory.getLogger(SlpDetailsRepositoryImpl.class);
	private ReactiveMongoOperations reactiveMongoOperations;

	public SlpDetailsRepositoryImpl(ReactiveMongoOperations reactiveMongoOperations) {
		this.reactiveMongoOperations = reactiveMongoOperations;
	}

	@Override
	public Maybe<SlpTokenDetails> fetchSlpDetails(SlpTokenId slpTokenId) {
		Query query = Query.query(Criteria.where("_id").is(slpTokenId.getHex()));
		return RxJava2Adapter.monoToMaybe(reactiveMongoOperations.findOne(query, SlpTokenDetailsDbObject.class))
				.map(e -> new SlpTokenDetails(new SlpTokenId(e.getTokenId()),
						e.getTokenTicker(),
						e.getName(),
						e.getDecimals(),
						e.getDocumentUri(),
						null));
	}

	@Override
	public Single<List<SlpTokenDetails>> fetchSlpDetails(List<SlpTokenId> slpTokenIds) {
		Query query = Query.query(Criteria.where("_id").in(slpTokenIds.stream().map(SlpTokenId::getHex).collect(Collectors.toList())));

		return RxJava2Adapter.fluxToFlowable(reactiveMongoOperations.find(query, SlpTokenDetailsDbObject.class)
				.doOnError(er -> logger.error("Error fetching batch details={}", slpTokenIds.stream().map(SlpTokenId::getHex).collect(Collectors.joining(":")), er)))
				.map(e -> new SlpTokenDetails(new SlpTokenId(e.getTokenId()),
						e.getTokenTicker(),
						e.getName(),
						e.getDecimals(),
						e.getDocumentUri(),
						null))
				.toList();
	}

	@Override
	public Single<SlpTokenDetails> saveSlpTokenDetails(SlpTokenDetails slpTokenDetails) {
		SlpTokenDetailsDbObject slpTokenDetailsDbObject = new SlpTokenDetailsDbObject(slpTokenDetails.getTokenId().getHex(),
				slpTokenDetails.getTicker(),
				slpTokenDetails.getName(),
				slpTokenDetails.getDecimals(),
				slpTokenDetails.getDocumentUri());
		Query query = Query.query(Criteria.where("_id").is(slpTokenDetailsDbObject.getTokenId()));

		return RxJava2Adapter.monoToMaybe(reactiveMongoOperations.upsert(query, slpTokenDetailsDbObject.toUpdate(), SlpTokenDetailsDbObject.class))
				.map(e -> slpTokenDetails)
				.toSingle(slpTokenDetails);
	}
}
