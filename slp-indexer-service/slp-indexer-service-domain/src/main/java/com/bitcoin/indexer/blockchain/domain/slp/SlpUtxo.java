package com.bitcoin.indexer.blockchain.domain.slp;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;

public class SlpUtxo implements Serializable {
	private static final long serialVersionUID = 1L;

	private final SlpTokenId slpTokenId;
	private final BigDecimal amount;
	private final boolean hasBaton;
	private final String tokenTicker;
	private final String tokenTransactionType;
	private final String slpTokenName;
	private final String tokenType;
	private final String tokenTypeHex;
	private final SlpValid parentTransactionValid;

	private SlpUtxo(SlpTokenId slpTokenId,
			BigDecimal amount,
			boolean hasBaton,
			String tokenTicker,
			String tokenTransactionType,
			String slpTokenName,
			String tokenType,
			String tokenTypeHex, SlpValid parentTransactionValid) {
		this.slpTokenId = Objects.requireNonNull(slpTokenId);
		this.amount = Objects.requireNonNull(amount);
		this.hasBaton = hasBaton;
		this.tokenTicker = Objects.requireNonNull(tokenTicker);
		this.tokenTransactionType = Objects.requireNonNull(tokenTransactionType);
		this.slpTokenName = Objects.requireNonNull(slpTokenName);
		this.tokenType = Objects.requireNonNull(tokenType);
		this.tokenTypeHex = Objects.requireNonNull(tokenTypeHex);
		this.parentTransactionValid = Objects.requireNonNull(parentTransactionValid);
	}

	public static SlpUtxo create(SlpTokenId slpTokenId,
			BigDecimal amount,
			boolean hasBaton,
			String tokenTicker,
			String tokenTransactionType,
			String slpTokenName,
			String tokenType,
			String tokenTypeHex,
			SlpValid parentTransactionValid) {
		return new SlpUtxo(slpTokenId,
				amount,
				hasBaton,
				tokenTicker,
				tokenTransactionType,
				slpTokenName,
				tokenType,
				tokenTypeHex, parentTransactionValid);
	}

	public static SlpUtxo send(SlpTokenId slpTokenId,
			BigDecimal amount,
			String tokenTicker,
			String slpTokenName,
			String tokenType,
			String tokenTypeHex) {
		return new SlpUtxo(slpTokenId,
				amount,
				false,
				tokenTicker,
				"SEND", slpTokenName, tokenType, tokenTypeHex, SlpValid.unknown());
	}

	public static SlpUtxo genesis(SlpTokenId slpTokenId,
			String tokenTicker,
			BigDecimal amount,
			boolean hasBaton,
			String slpTokenName,
			String tokenType,
			String tokenTypeHex) {
		return new SlpUtxo(slpTokenId,
				amount,
				hasBaton,
				tokenTicker,
				"GENESIS", slpTokenName, tokenType, tokenTypeHex, SlpValid.unknown());
	}

	public static SlpUtxo mint(SlpTokenId slpTokenId,
			BigDecimal amount,
			boolean hasBaton,
			String tokenTicker,
			String slpTokenName,
			String tokenType,
			String tokenTypeHex) {
		return new SlpUtxo(slpTokenId,
				amount,
				hasBaton,
				tokenTicker,
				"MINT", slpTokenName, tokenType, tokenTypeHex, SlpValid.unknown());
	}

	public SlpUtxo withValid(SlpValid slpValid) {
		return SlpUtxo.create(
				slpTokenId,
				amount,
				hasBaton,
				tokenTicker,
				tokenTransactionType,
				slpTokenName,
				tokenType,
				tokenTypeHex,
				slpValid
		);
	}

	public boolean isGenesis() {
		return tokenTransactionType.equals("GENESIS");
	}

	public boolean isMint() {
		return tokenTransactionType.equals("MINT");
	}

	public SlpTokenId getSlpTokenId() {
		return slpTokenId;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public boolean hasBaton() {
		return hasBaton;
	}

	public String getTokenTicker() {
		return tokenTicker;
	}

	public boolean isHasBaton() {
		return hasBaton;
	}

	public String getTokenTransactionType() {
		return tokenTransactionType;
	}

	public String getSlpTokenName() {
		return slpTokenName;
	}

	public String getTokenType() {
		return tokenType;
	}

	public String getTokenTypeHex() {
		return tokenTypeHex;
	}

	public SlpValid getParentTransactionValid() {
		return parentTransactionValid;
	}

	@Override
	public String toString() {
		return "SlpUtxo [" +
				"slpTokenId=" + slpTokenId +
				", amount=" + amount +
				", hasBaton=" + hasBaton +
				", tokenTicker=" + tokenTicker +
				", tokenTransactionType=" + tokenTransactionType +
				", slpTokenName=" + slpTokenName +
				", tokenType=" + tokenType +
				']';
	}
}
