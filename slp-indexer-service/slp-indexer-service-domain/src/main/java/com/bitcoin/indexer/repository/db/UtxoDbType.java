package com.bitcoin.indexer.repository.db;

import java.io.Serializable;
import java.util.Optional;

import org.bson.Document;

import com.bitcoin.indexer.blockchain.domain.Utxo;

public class UtxoDbType implements Serializable {
	private static final long serialVersionUID = 1L;

	private String txId;

	private String address;

	private String scriptPubkey;

	private String amount;

	private boolean confirmations;

	private int utxoIndex;

	private boolean isOpReturn;

	private SlpUtxoType slpUtxoType;

	public UtxoDbType(String txId, String address, String scriptPubkey, String amount, boolean confirmations, int utxoIndex, boolean isOpReturn, SlpUtxoType slpUtxoType) {
		this.txId = txId;
		this.address = address;
		this.scriptPubkey = scriptPubkey;
		this.amount = amount;
		this.confirmations = confirmations;
		this.utxoIndex = utxoIndex;
		this.isOpReturn = isOpReturn;
		this.slpUtxoType = slpUtxoType;
	}

	public UtxoDbType() {
	}

	public static UtxoDbType fromDomain(Utxo utxo) {
		return new UtxoDbType(utxo.getTxId(), utxo.getAddress().getAddress(), utxo.getScriptPubkey(), utxo.getAmount().toString(), utxo.isConfirmed(), utxo.getIndex(),
				utxo.isOpReturn(),
				utxo.getSlpUtxo().map(u -> new SlpUtxoType(u.getSlpTokenId().toString(),
						u.getTokenTransactionType(),
						u.getTokenType(),
						u.getAmount().toString(),
						u.hasBaton(),
						u.getTokenTicker(),
						u.getSlpTokenName(),
						SlpValidDbType.fromDomain(u.getParentTransactionValid()),
						u.getTokenTypeHex()))
						.orElse(null));
	}

	public String getTxId() {
		return txId;
	}

	public void setTxId(String txId) {
		this.txId = txId;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public String getScriptPubkey() {
		return scriptPubkey;
	}

	public void setScriptPubkey(String scriptPubkey) {
		this.scriptPubkey = scriptPubkey;
	}

	public String getAmount() {
		return amount;
	}

	public void setAmount(String amount) {
		this.amount = amount;
	}

	public boolean isConfirmations() {
		return confirmations;
	}

	public void setConfirmations(boolean confirmations) {
		this.confirmations = confirmations;
	}

	public int getUtxoIndex() {
		return utxoIndex;
	}

	public void setUtxoIndex(int utxoIndex) {
		this.utxoIndex = utxoIndex;
	}

	public void setOpReturn(boolean opReturn) {
		isOpReturn = opReturn;
	}

	public void setSlpUtxoType(SlpUtxoType slpUtxoType) {
		this.slpUtxoType = slpUtxoType;
	}

	public int getIndex() {
		return utxoIndex;
	}

	public void setIndex(int index) {
		this.utxoIndex = index;
	}

	public Optional<SlpUtxoType> getSlpUtxoType() {
		return Optional.ofNullable(slpUtxoType);
	}

	public boolean isOpReturn() {
		return isOpReturn;
	}

	/*private String txId;

	private String address;

	private String scriptPubkey;

	private String amount;

	private boolean confirmations;

	private int utxoIndex;

	private boolean isOpReturn;

	private SlpUtxoType slpUtxoType;*/

	public Document toDocument() {
		Document update = new Document();
		update.put("txId", txId);
		update.put("address", address);
		update.put("scriptPubkey", scriptPubkey);
		update.put("amount", amount);
		update.put("confirmations", confirmations);
		update.put("utxoIndex", utxoIndex);
		update.put("isOpReturn", isOpReturn);
		if (slpUtxoType != null) {
			update.put("slpUtxoType", slpUtxoType.toDocument());
		}
		return update;
	}
}
