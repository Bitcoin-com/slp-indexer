package com.bitcoin.indexer.blockchain.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public class Block {

	private final List<Transaction> transactions;
	private final String hash;
	private final long height;
	private final String prevBlockHash;
	private final String coinbaseString;
	private final List<String> txIds;
	private final String merkelRoot;
	private final long nounce;
	private final BigDecimal blockReward;
	private final String chainWork;
	private final BigDecimal difficulty;
	private final long size;
	private final long version;
	private final Instant time;
	private final String bits;

	public Block(List<Transaction> transactions,
			String hash,
			long height,
			String prevBlockHash,
			String coinbaseString,
			List<String> txIds,
			String merkelRoot,
			long nounce,
			BigDecimal blockReward,
			String chainWork,
			BigDecimal difficulty,
			long size,
			long version,
			Instant time,
			String bits) {
		this.transactions = List.copyOf(transactions);
		this.hash = Objects.requireNonNull(hash);
		this.height = height;
		this.prevBlockHash = Objects.requireNonNull(prevBlockHash);
		this.coinbaseString = Objects.requireNonNull(coinbaseString);
		this.txIds = List.copyOf(txIds);
		this.merkelRoot = merkelRoot;
		this.nounce = nounce;
		this.blockReward = blockReward;
		this.chainWork = chainWork;
		this.difficulty = difficulty;
		this.size = size;
		this.version = version;
		this.time = time;
		this.bits = bits;
	}

	public List<Transaction> getTransactions() {
		return transactions;
	}

	public String getHash() {
		return hash;
	}

	public long getHeight() {
		return height;
	}

	public String getPrevBlockHash() {
		return prevBlockHash;
	}

	public String getCoinbaseString() {
		return coinbaseString;
	}

	public List<String> getTxIds() {
		return txIds;
	}

	public String getMerkelRoot() {
		return merkelRoot;
	}

	public long getNounce() {
		return nounce;
	}

	public BigDecimal getBlockReward() {
		return blockReward;
	}

	public String getChainWork() {
		return chainWork;
	}

	public BigDecimal getDifficulty() {
		return difficulty;
	}

	public long getSize() {
		return size;
	}

	public long getVersion() {
		return version;
	}

	public Instant getTime() {
		return time;
	}

	public String getBits() {
		return bits;
	}

	@Override
	public String toString() {
		return "Block [" +
				"transactions=" + transactions +
				", hash=" + hash +
				", height=" + height +
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
		Block block = (Block) o;
		return height == block.height &&
				Objects.equals(hash, block.hash);
	}

	@Override
	public int hashCode() {
		return Objects.hash(hash, height);
	}
}
