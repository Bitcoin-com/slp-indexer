package com.bitcoin.indexer.blockchain.domain.slp;

import java.util.Objects;
import java.util.Optional;

public class ExtendedDetails {

	private final SlpTokenDetails slpTokenDetails;
	private final Integer lastActiveMint;
	private final Integer lastActiveSend;
	private final Integer blockCreated;

	private ExtendedDetails(SlpTokenDetails slpTokenDetails, Integer lastActiveMint, Integer lastActiveSend, Integer blockCreated) {
		this.slpTokenDetails = Objects.requireNonNull(slpTokenDetails);
		this.blockCreated = Objects.requireNonNull(blockCreated);
		this.lastActiveMint = lastActiveMint;
		this.lastActiveSend = lastActiveSend;
	}

	public static ExtendedDetails create(SlpTokenDetails slpTokenDetails, Integer lastActiveMint, Integer lastActiveSend, Integer blockCreated) {
		return new ExtendedDetails(slpTokenDetails, lastActiveMint, lastActiveSend, blockCreated);
	}

	public SlpTokenDetails getSlpTokenDetails() {
		return slpTokenDetails;
	}

	public Optional<Integer> getLastActiveMint() {
		return Optional.ofNullable(lastActiveMint);
	}

	public Optional<Integer> getLastActiveSend() {
		return Optional.ofNullable(lastActiveSend);
	}

	public Integer getBlockCreated() {
		return blockCreated;
	}
}
