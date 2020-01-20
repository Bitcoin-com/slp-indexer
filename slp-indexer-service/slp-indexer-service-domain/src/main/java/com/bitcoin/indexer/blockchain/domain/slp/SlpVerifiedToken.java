package com.bitcoin.indexer.blockchain.domain.slp;

import java.util.Objects;

public class SlpVerifiedToken {

	private final SlpTokenId slpTokenId;

	private final boolean verified;

	private final String imageURL;

	private final String imageHexData;

	private SlpVerifiedToken(SlpTokenId slpTokenId, boolean verified, String imageURL, String imageHexData) {
		this.slpTokenId = Objects.requireNonNull(slpTokenId);
		this.verified = verified;
		this.imageURL = Objects.requireNonNull(imageURL);
		this.imageHexData = Objects.requireNonNull(imageHexData);
	}

	public static SlpVerifiedToken create(SlpTokenId slpTokenId, boolean verified, String imageURL, String imageHexData) {
		return new SlpVerifiedToken(slpTokenId, verified, imageURL, imageHexData);
	}

	public SlpTokenId getSlpTokenId() {
		return slpTokenId;
	}

	public boolean isVerified() {
		return verified;
	}

	public String getImageURL() {
		return imageURL;
	}

	public String getImageHexData() {
		return imageHexData;
	}
}
