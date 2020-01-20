package com.bitcoin.indexer.blockchain.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

import com.bitcoin.indexer.core.Coin;

public class AddressTransaction {

	private final Address address;

	private final String txId;

	private final Coin coin;

	private final String fromTxId;

	private final int fromTxIndex;

	private final Instant timestamp;

	private final BigDecimal transactionValue;

	private final String confirmedBlockHash;

	private AddressTransaction(Address address, String txId, Coin coin, String fromTxId, int fromTxIndex, Instant timestamp, BigDecimal transactionValue, String confirmedBlockHash) {
		this.address = Objects.requireNonNull(address);
		this.txId = Objects.requireNonNull(txId);
		this.coin = Objects.requireNonNull(coin);
		this.fromTxId = Objects.requireNonNull(fromTxId);
		this.fromTxIndex = fromTxIndex;
		this.timestamp = Objects.requireNonNull(timestamp);
		this.transactionValue = Objects.requireNonNull(transactionValue);
		this.confirmedBlockHash = confirmedBlockHash;
	}

	public static AddressTransaction create(Address address, String txId, Coin coin, String fromTxId, int fromTxIndex, Instant timestamp, BigDecimal transactionValue, String confirmedBlockHash) {
		return new AddressTransaction(address, txId, coin, fromTxId, fromTxIndex, timestamp, transactionValue, confirmedBlockHash);
	}

	public Address getAddress() {
		return address;
	}

	public String getTxId() {
		return txId;
	}

	public Coin getCoin() {
		return coin;
	}

	public String getFromTxId() {
		return fromTxId;
	}

	public int getFromTxIndex() {
		return fromTxIndex;
	}

	public Instant getTimestamp() {
		return timestamp;
	}

	public BigDecimal getTransactionValue() {
		return transactionValue;
	}

	public Optional<String> getConfirmedBlockHash() {
		return Optional.ofNullable(confirmedBlockHash);
	}
}
