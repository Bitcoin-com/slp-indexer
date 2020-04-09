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

	private Integer lastActiveSend;

	private Integer lastActiveMint;

	private Integer blockCreated;

	public SlpTokenDetailsDbObject(String tokenId, String tokenTicker, String name, Integer decimals, String documentUri, Integer lastActiveSend, Integer lastActiveMint, Integer blockCreated) {
		this.tokenId = tokenId;
		this.tokenTicker = tokenTicker;
		this.name = name;
		this.decimals = decimals;
		this.documentUri = documentUri;
		this.lastActiveSend = lastActiveSend;
		this.lastActiveMint = lastActiveMint;
		this.blockCreated = blockCreated;
	}

	public Update toUpdate() {
		Update update = new Update();
		update.set("tokenId", tokenId);
		update.set("tokenTicker", tokenTicker);
		update.set("name", name);
		update.set("decimals", decimals);
		update.set("documentUri", documentUri);
		update.set("lastActiveSend", lastActiveSend);
		update.set("lastActiveMint", lastActiveMint);
		update.set("blockCreated", blockCreated);
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

	public Integer getLastActiveSend() {
		return lastActiveSend;
	}

	public Integer getLastActiveMint() {
		return lastActiveMint;
	}

	public Integer getBlockCreated() {
		return blockCreated;
	}
}
