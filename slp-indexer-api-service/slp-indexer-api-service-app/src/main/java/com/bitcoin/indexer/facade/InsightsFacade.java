package com.bitcoin.indexer.facade;

import java.util.Optional;

public interface InsightsFacade {

	Optional<InsightsResponse> getTxInfo(String txId);
}
