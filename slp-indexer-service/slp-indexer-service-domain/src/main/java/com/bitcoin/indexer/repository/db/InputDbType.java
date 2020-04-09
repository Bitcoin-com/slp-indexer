package com.bitcoin.indexer.repository.db;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Optional;

import org.bson.Document;

import com.bitcoin.indexer.blockchain.domain.Input;

public class InputDbType implements Serializable {
	private static final long serialVersionUID = 1L;

	private String address;

	private String amount;

	private int inputIndex;

	private String txId;

	private boolean isCoinbase;

	private long sequence;

	//Naming error :(
	private SlpUtxoType slpTokenType;

	public InputDbType(String address, String amount, int inputIndex, String txId, SlpUtxoType slpTokenType, boolean isCoinbase, long sequence) {
		this.address = address;
		this.amount = amount;
		this.inputIndex = inputIndex;
		this.txId = txId;
		this.slpTokenType = slpTokenType;
		this.isCoinbase = isCoinbase;
		this.sequence = sequence;
	}

	public static InputDbType fromDomain(Input input) {
		return new InputDbType(
				input.getAddress().getAddress(),
				input.getAmount().map(BigDecimal::toString).orElse("0"),
				input.getIndex(),
				input.getTxId(),
				input.getSlpUtxo().map(slp -> new SlpUtxoType(
						slp.getSlpTokenId().getHex(),
						slp.getTokenTransactionType(),
						slp.getTokenType(),
						slp.getAmount().toString(),
						slp.hasBaton(),
						slp.getTokenTicker(),
						slp.getSlpTokenName(),
						SlpValidDbType.fromDomain(slp.getParentTransactionValid()),
						slp.getTokenTypeHex())).orElse(null),
				input.isCoinbase(),
				input.getSequence()
		);
	}

	public String getAddress() {
		return address;
	}

	public String getAmount() {
		return amount;
	}

	public int getInputIndex() {
		return inputIndex;
	}

	public String getTxId() {
		return txId;
	}

	public Optional<SlpUtxoType> getSlpUtxoType() {
		return Optional.ofNullable(slpTokenType);
	}

	public boolean isCoinbase() {
		return isCoinbase;
	}

	public long getSequence() {
		return sequence;
	}

	public Document toDocument() {
		Document document = new Document();
		document.put("address", address);
		document.put("amount", amount);
		document.put("inputIndex", inputIndex);
		document.put("txId", txId);
		document.put("slpTokenType", slpTokenType == null ? null : slpTokenType.toDocument());
		document.put("isCoinbase", isCoinbase);
		document.put("sequence", sequence);
		return document;
	}
}
