package com.bitcoin.indexer.repository.db;

import java.math.BigDecimal;
import java.time.Instant;

import org.springframework.data.annotation.Id;

public class AddressTransactionDbObject {

	@Id
	private String i;

	private String tx;

	private String a;

	private String f;

	private int fi;

	private Instant t;

	private BigDecimal tv;

	private String c;

	public AddressTransactionDbObject() {
	}

	public AddressTransactionDbObject(String txId, String address, String fromTxId, int fromTxIndex, Instant timestamp, BigDecimal transactionValue, String confirmedBlockHash) {
		this.i = txId + ":" + address;
		this.tx = txId;
		this.a = address;
		this.f = fromTxId;
		this.fi = fromTxIndex;
		this.t = timestamp;
		this.tv = transactionValue;
		this.c = confirmedBlockHash;
	}

	public String getTx() {
		return tx;
	}

	public String getA() {
		return a;
	}

	public String getF() {
		return f;
	}

	public int getFi() {
		return fi;
	}

	public Instant getT() {
		return t;
	}

	public BigDecimal getTv() {
		return tv;
	}

	public String getC() {
		return c;
	}
}
