package com.bitcoin.indexer.blockchain.domain;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

import com.bitcoin.indexer.blockchain.domain.slp.SlpUtxo;
import com.bitcoin.indexer.blockchain.domain.slp.SlpValid;

public class Utxo implements Comparable<Utxo>, Serializable {
	private static final long serialVersionUID = 1L;
	private final String txId;
	private final Address address;
	private final String scriptPubkey;
	private final BigDecimal amount;
	private final boolean confirmed;
	private final int index;
	private final boolean isSpent;
	private final SlpUtxo slpUtxo;
	private final Instant timestamp;
	private final boolean isOpReturn;
	private final Integer confirmedHeight;

	public Utxo(String txId, Address address, String scriptPubkey, BigDecimal amount, boolean confirmed, int index, boolean isSpent, Instant timestamp, SlpUtxo slpUtxo, boolean isOpReturn, Integer confirmedHeight) {
		this.txId = Objects.requireNonNull(txId);
		this.address = Objects.requireNonNull(address);
		this.scriptPubkey = Objects.requireNonNull(scriptPubkey);
		this.amount = Objects.requireNonNull(amount);
		this.timestamp = Objects.requireNonNull(timestamp);
		this.confirmed = confirmed;
		this.index = index;
		this.isSpent = isSpent;
		this.slpUtxo = slpUtxo;
		this.isOpReturn = isOpReturn;
		this.confirmedHeight = confirmedHeight;
	}

	public static Utxo create(String txId,
			Address address,
			String scriptPubkey,
			BigDecimal amount,
			boolean confirmations,
			int index,
			boolean isSpent,
			Instant timestamp,
			SlpUtxo slpUtxo,
			boolean isOpReturn,
			Integer confirmedHeight) {
		return new Utxo(txId, address, scriptPubkey, amount, confirmations, index, isSpent, timestamp, slpUtxo, isOpReturn, confirmedHeight);
	}

	public static Utxo unconfirmed(String txId,
			Address address,
			String scriptPubkey,
			BigDecimal amount,
			Instant timestamp,
			int index,
			boolean isOpReturn) {
		return new Utxo(txId, address, scriptPubkey, amount, false, index, false, timestamp, null, isOpReturn, null);
	}

	public static Utxo confirmed(String txId,
			Address address,
			String scriptPubkey,
			BigDecimal amount,
			Instant timestamp,
			int index,
			boolean isOpReturn,
			Integer confirmedHeight) {
		return new Utxo(txId, address, scriptPubkey, amount, true, index, false, timestamp, null, isOpReturn, confirmedHeight);
	}

	public Utxo withTimestamp(Instant instant) {
		return Utxo.create(
				txId,
				address,
				scriptPubkey,
				amount,
				confirmed,
				index,
				isSpent,
				instant,
				slpUtxo,
				isOpReturn,
				confirmedHeight
		);
	}

	public Utxo withValid(SlpValid slpValid) {
		return Utxo.create(
				txId,
				address,
				scriptPubkey,
				amount,
				confirmed,
				index,
				isSpent,
				timestamp,
				Optional.ofNullable(slpUtxo).map(slpUtxo -> slpUtxo.withValid(slpValid)).orElse(null),
				isOpReturn,
				confirmedHeight
		);
	}

	public boolean isConfirmed() {
		return confirmed;
	}

	public String getTxId() {
		return txId;
	}

	public Address getAddress() {
		return address;
	}

	public String getScriptPubkey() {
		return scriptPubkey;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public int getIndex() {
		return index;
	}

	public boolean isSpent() {
		return isSpent;
	}

	public boolean isOpReturn() {
		return isOpReturn;
	}

	public Optional<SlpUtxo> getSlpUtxo() {
		return Optional.ofNullable(slpUtxo);
	}

	public Instant getTimestamp() {
		return timestamp;
	}

	public Optional<Integer> getConfirmedHeight() {
		return Optional.ofNullable(confirmedHeight);
	}

	@Override
	public String toString() {
		return "Utxo [" +
				"txId=" + txId +
				", address=" + address +
				", scriptPubkey=" + scriptPubkey +
				", amount=" + amount +
				", confirmed=" + confirmed +
				", index=" + index +
				", isSpent=" + isSpent +
				", slpUtxo=" + slpUtxo +
				", timestamp=" + timestamp +
				", isOpReturn=" + isOpReturn +
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
		Utxo utxo = (Utxo) o;
		return index == utxo.index &&
				Objects.equals(txId, utxo.txId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(txId, index);
	}

	@Override
	public int compareTo(Utxo o) {
		return Integer.compare(this.getIndex(), o.index);
	}
}
