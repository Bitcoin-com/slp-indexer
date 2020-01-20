package com.bitcoin.indexer.responses;

import java.math.BigDecimal;

public class ExtendedDetailsResponse {

	public int decimals;
	public String documentUri;
	public String symbol;
	public String name;
	public boolean containsBaton;
	public String id;
	public BigDecimal initialTokenQty;
	public long blockCreated;
	public BigDecimal quantity;
	public BigDecimal validTokenUtxos;
	public int validTokenAddresses;
	public BigDecimal satoshisLockedUp;
	public Integer lastActiveSend;
	public Integer activeMint;

	public ExtendedDetailsResponse() {
	}

	public ExtendedDetailsResponse(int decimals,
			String documentUri,
			String symbol,
			String name,
			boolean containsBaton,
			String id,
			BigDecimal initialTokenQty,
			long blockCreated,
			BigDecimal quantity,
			BigDecimal validTokenUtxos,
			int validTokenAddresses,
			BigDecimal satoshisLockedUp, Integer lastActiveSend, Integer activeMint) {
		this.decimals = decimals;
		this.documentUri = documentUri;
		this.symbol = symbol;
		this.name = name;
		this.containsBaton = containsBaton;
		this.id = id;
		this.initialTokenQty = initialTokenQty;
		this.blockCreated = blockCreated;
		this.quantity = quantity;
		this.validTokenUtxos = validTokenUtxos;
		this.validTokenAddresses = validTokenAddresses;
		this.satoshisLockedUp = satoshisLockedUp;
		this.lastActiveSend = lastActiveSend;
		this.activeMint = activeMint;
	}
}
