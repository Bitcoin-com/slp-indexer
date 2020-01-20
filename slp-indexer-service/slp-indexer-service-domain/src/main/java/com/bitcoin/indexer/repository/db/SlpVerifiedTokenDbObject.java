package com.bitcoin.indexer.repository.db;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "verified_token")
public class SlpVerifiedTokenDbObject {

	@Id
	private String tokenId;

	private Boolean verified;

	private String imageURL;

	private String imageHexData;

	public SlpVerifiedTokenDbObject(String tokenId, Boolean verified, String imageURL, String imageHexData) {
		this.tokenId = tokenId;
		this.verified = verified;
		this.imageURL = imageURL;
		this.imageHexData = imageHexData;
	}

	public String getTokenId() {
		return tokenId;
	}

	public Boolean getVerified() {
		return verified;
	}

	public String getImageURL() {
		return imageURL;
	}

	public String getImageHexData() {
		return imageHexData;
	}
}
