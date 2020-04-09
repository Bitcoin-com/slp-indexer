package com.bitcoin.indexer.blockchain.domain;

import java.math.BigDecimal;
import java.util.Objects;

public class UtxoMinimalData {
	private final String txId;
	private final BigDecimal amount;
	private final boolean hasBaton;
	private final Address address;
	private final BigDecimal satoshisValue;
	private final String tokenTransactionType;
	private final String tokenId;

	public UtxoMinimalData(String txId, BigDecimal amount, boolean hasBaton, Address address, BigDecimal satoshisValue, String tokenTransactionType, String tokenId) {
		this.txId = Objects.requireNonNull(txId);
		this.amount = Objects.requireNonNull(amount);
		this.hasBaton = hasBaton;
		this.address = Objects.requireNonNull(address);
		this.satoshisValue = Objects.requireNonNull(satoshisValue);
		this.tokenTransactionType = Objects.requireNonNull(tokenTransactionType);
		this.tokenId = Objects.requireNonNull(tokenId);
	}

	public String getTxId() {
		return txId;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public boolean isHasBaton() {
		return hasBaton;
	}

	public Address getAddress() {
		return address;
	}

	public BigDecimal getSatoshisValue() {
		return satoshisValue;
	}

	public String getTokenTransactionType() {
		return tokenTransactionType;
	}

	public boolean isMint() {
		return tokenTransactionType.equals("MINT");
	}

	public String getTokenId() {
		return tokenId;
	}
}
