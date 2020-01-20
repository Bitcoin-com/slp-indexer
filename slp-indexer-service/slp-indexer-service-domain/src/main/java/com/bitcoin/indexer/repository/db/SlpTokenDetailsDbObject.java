package com.bitcoin.indexer.repository.db;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.query.Update;

@Document(collection = "token_details")
public class SlpTokenDetailsDbObject {

	@Id
	private String tokenId;

	private String tokenTicker;

	private String name;

	private Integer decimals;

	private String documentUri;

	public SlpTokenDetailsDbObject(String tokenId, String tokenTicker, String name, Integer decimals, String documentUri) {
		this.tokenId = tokenId;
		this.tokenTicker = tokenTicker;
		this.name = name;
		this.decimals = decimals;
		this.documentUri = documentUri;
	}

	public Update toUpdate() {
		Update update = new Update();
		update.set("tokenId", tokenId);
		update.set("tokenTicker", tokenTicker);
		update.set("name", name);
		update.set("decimals", decimals);
		update.set("documentUri", documentUri);
		return update;
	}

	public String getTokenId() {
		return tokenId;
	}

	public String getTokenTicker() {
		return tokenTicker;
	}

	public String getName() {
		return name;
	}

	public Integer getDecimals() {
		return decimals;
	}

	public String getDocumentUri() {
		return documentUri;
	}
}
