package com.bitcoin.indexer.blockchain.domain;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;

import com.bitcoin.indexer.blockchain.domain.slp.SlpUtxo;

public class Input {

	private final Address address;
	private final BigDecimal amount;
	private final int index;
	private final String txId;
	private final SlpUtxo slpTokenType;
	private final boolean isCoinbase;
	private final long sequence;

	private Input(Address address, BigDecimal amount, int index, String txId, SlpUtxo slpTokenType, boolean isCoinbase, long sequence) {
		this.address = Objects.requireNonNull(address);
		this.amount = amount;
		this.index = index;
		this.txId = Objects.requireNonNull(txId);
		this.slpTokenType = slpTokenType;
		this.isCoinbase = isCoinbase;
		this.sequence = sequence;
	}

	public static Input knownValue(Address address, BigDecimal amount, int index, String txId, SlpUtxo slpUtxo, boolean isCoinbase, long sequence) {
		return new Input(address, amount, index, txId, slpUtxo, isCoinbase, sequence);
	}

	public static Input unknownValue(Address address, int index, String txId, boolean isCoinbase, long sequence) {
		return new Input(address, null, index, txId, null, isCoinbase, sequence);
	}

	public Address getAddress() {
		return address;
	}

	public Optional<BigDecimal> getAmount() {
		return Optional.ofNullable(amount);
	}

	public int getIndex() {
		return index;
	}

	public String getTxId() {
		return txId;
	}

	public Optional<SlpUtxo> getSlpUtxo() {
		return Optional.ofNullable(slpTokenType);
	}

	public boolean isCoinbase() {
		return isCoinbase;
	}

	public long getSequence() {
		return sequence;
	}

	@Override
	public String toString() {
		return "Input [" +
				"address=" + address +
				", amount=" + amount +
				", index=" + index +
				", txId=" + txId +
				", slpTokenType=" + slpTokenType +
				", isCoinbase=" + isCoinbase +
				", sequence=" + sequence +
				']';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		Input input = (Input) o;
		return index == input.index &&
				Objects.equals(address, input.address) &&
				Objects.equals(amount, input.amount) &&
				Objects.equals(txId, input.txId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(address, amount, index, txId);
	}
}
