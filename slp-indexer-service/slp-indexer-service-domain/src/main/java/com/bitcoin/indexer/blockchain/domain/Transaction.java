package com.bitcoin.indexer.blockchain.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.bitcoin.indexer.blockchain.domain.slp.SlpOpReturn;
import com.bitcoin.indexer.blockchain.domain.slp.SlpValid;

public class Transaction {

	private final List<Utxo> outputs;
	private final List<Input> inputs;
	private final String txId;
	private final boolean confirmed;
	private final BigDecimal fees;
	private final Instant time;
	private final boolean fromBlock;
	private final String blockHash;
	private final Integer blockHeight;
	private final List<SlpOpReturn> slpOpReturn;
	private final SlpValid slpValid;
	private final String rawHex;
	private final long version;
	private final long locktime;
	private final long size;
	private final Instant blockTime;

	private Transaction(String txId,
			List<Utxo> outputs,
			List<Input> inputs,
			boolean confirmed,
			BigDecimal fees,
			Instant time,
			boolean fromBlock,
			String blockHash,
			Integer blockHeight,
			List<SlpOpReturn> slpOpReturn,
			SlpValid slpValid,
			String rawHex,
			long version,
			long locktime,
			long size,
			Instant blockTime) {
		this.outputs = List.copyOf(outputs);
		this.txId = Objects.requireNonNull(txId);
		this.inputs = List.copyOf(inputs);
		this.confirmed = confirmed;
		this.fees = Objects.requireNonNull(fees);
		this.time = Objects.requireNonNull(time);
		this.fromBlock = fromBlock;
		this.blockHash = blockHash;
		this.blockHeight = blockHeight;
		this.slpOpReturn = List.copyOf(slpOpReturn);
		this.slpValid = slpValid;
		this.rawHex = Objects.requireNonNull(rawHex);
		this.version = version;
		this.locktime = locktime;
		this.size = size;
		this.blockTime = blockTime;

		if (inputs.isEmpty()) {
			throw new IllegalArgumentException("Tx can not exist without inputs txId=" + txId);
		}

		if (outputs.isEmpty()) {
			throw new IllegalArgumentException("Tx can not exist without outputs txId=" + txId);
		}
	}

	public static Transaction fromMempool(String txId,
			List<Utxo> outputs,
			List<Input> inputs,
			BigDecimal fees,
			Instant time,
			List<SlpOpReturn> slpOpReturn,
			SlpValid slpValid,
			String rawHex,
			long version,
			long locktime,
			long size) {
		return new Transaction(txId,
				outputs,
				inputs,
				false,
				fees,
				time,
				false,
				null,
				null,
				slpOpReturn,
				slpValid,
				rawHex,
				version,
				locktime,
				size,
				null);
	}

	public static Transaction fromBlock(String txId,
			List<Utxo> outputs,
			List<Input> inputs,
			boolean confirmed,
			BigDecimal fees,
			Instant time,
			boolean fromBlock,
			String blockHash,
			int blockHeight,
			List<SlpOpReturn> slpOpReturn,
			SlpValid slpValid,
			String rawHex,
			long version,
			long locktime,
			long size,
			Instant blockTime) {
		return new Transaction(txId,
				outputs,
				inputs,
				confirmed,
				fees,
				time,
				fromBlock,
				blockHash,
				blockHeight,
				slpOpReturn,
				slpValid,
				rawHex,
				version,
				locktime,
				size,
				blockTime);

	}

	public static Transaction create(String txId,
			List<Utxo> outputs,
			List<Input> inputs,
			boolean confirmed,
			BigDecimal fees,
			Instant time,
			boolean fromBlock,
			String blockHash,
			Integer blockHeight,
			List<SlpOpReturn> slpOpReturn,
			SlpValid slpValid,
			String rawHex, long version, long locktime, long size, Instant blockTime) {
		return new Transaction(txId, outputs, inputs, confirmed, fees, time, fromBlock, blockHash, blockHeight, slpOpReturn, slpValid, rawHex, version, locktime, size, blockTime);
	}

	public boolean isConfirmed() {
		return confirmed;
	}

	public BigDecimal getFees() {
		return fees;
	}

	public Instant getTime() {
		return time;
	}

	public List<Utxo> getOutputs() {
		return outputs;
	}

	public String getTxId() {
		return txId;
	}

	public List<Input> getInputs() {
		return inputs;
	}

	public boolean isFromBlock() {
		return fromBlock;
	}

	public Optional<String> getBlockHash() {
		return Optional.ofNullable(blockHash);
	}

	public Optional<Integer> getBlockHeight() {
		return Optional.ofNullable(blockHeight);
	}

	public List<SlpOpReturn> getSlpOpReturn() {
		return slpOpReturn;
	}

	public boolean isSlp() {
		return !slpOpReturn.isEmpty();
	}

	public Optional<SlpValid> getSlpValid() {
		return Optional.ofNullable(slpValid);
	}

	public long getVersion() {
		return version;
	}

	public long getLocktime() {
		return locktime;
	}

	public long getSize() {
		return size;
	}

	public Optional<Instant> getBlockTime() {
		return Optional.ofNullable(blockTime);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		Transaction that = (Transaction) o;
		return confirmed == that.confirmed &&
				fromBlock == that.fromBlock &&
				Objects.equals(outputs, that.outputs) &&
				Objects.equals(inputs, that.inputs) &&
				Objects.equals(txId, that.txId) &&
				Objects.equals(fees, that.fees) &&
				Objects.equals(time, that.time) &&
				Objects.equals(blockHash, that.blockHash) &&
				Objects.equals(blockHeight, that.blockHeight);
	}

	@Override
	public int hashCode() {
		return Objects.hash(outputs, inputs, txId, confirmed, fees, time, fromBlock, blockHash, blockHeight);
	}

	@Override
	public String toString() {
		return "Transaction [" +
				"outputs=" + outputs +
				", inputs=" + inputs +
				", txId=" + txId +
				", slpOpReturn=" + slpOpReturn +
				']';
	}

	public String getRawHex() {
		return rawHex;
	}
}
