package com.bitcoin.indexer.repository.db;

import java.math.BigDecimal;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.query.Update;

@Document(collection = "token_transactions")
public class TokenTransactionsDbObject {

	@Id
	private String tokenId;

	private BigDecimal transactions;

	public TokenTransactionsDbObject(String tokenId, BigDecimal transactions) {
		this.tokenId = tokenId;
		this.transactions = transactions;
	}

	public Update toUpdate() {
		Update update = new Update();
		update.set("tokenId", tokenId);
		update.set("transactions", transactions);
		return update;
	}

	public String getTokenId() {
		return tokenId;
	}

	public BigDecimal getTransactions() {
		return transactions;
	}
}
