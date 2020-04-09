package com.bitcoin.indexer.responses;

import java.math.BigDecimal;
import java.util.Optional;

import org.spongycastle.util.encoders.Hex;

import com.bitcoin.indexer.blockchain.domain.slp.ByteUtils;

public class ExtendedDetailsResponse {
	/*
	* {
    "decimals": 8,
    "timestamp": "2019-08-05 10:05:48",
    "versionType": 1,
    "documentUri": "developer.bitcoin.com",
    "symbol": "SLPCF",
    "name": "SLP SDK example CF",
    "containsBaton": true,
    "id": "22e0a1c63e534ee2f2cfa607318788bcde8e630c6b0511529d4a04c9ea389721",
    "documentHash": null,
    "initialTokenQty": 20,
    "blockCreated": 1319608,
    "blockLastActiveSend": 1326137,
    "blockLastActiveMint": 1326137,
    "txnsSinceGenesis": 29,
    "validAddresses": 8,
    "totalMinted": 31,
    "totalBurned": 18.4571,
    "circulatingSupply": 12.5429,
    "mintingBatonStatus": "ALIVE",
    "timestampUnix": 1564999548
  }*/

	public int decimals;
	public String documentUri;
	public String documentHash;
	public String symbol;
	public String name;
	public boolean containsBaton;
	public String id;
	public BigDecimal initialTokenQty;
	public long blockCreated;
	public BigDecimal quantity;
	public Integer lastActiveSend;
	public Integer activeMint;
	public Integer versionType;
	public Long timestampUnix;
	public String timestamp;
	public Long totalMinted;
	public Long totalBurned;
	public String circulatingSupply;
	public BigDecimal validTokenUtxos;
	public Long validAddresses;
	public BigDecimal satoshisLockedUp;
	public BigDecimal txnsSinceGenesis;
	public String mintingBatonStatus;
	public Integer blockLastActiveSend;
	public Integer blockLastActiveMint;

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
			Integer lastActiveSend,
			Integer activeMint,
			String versionType,
			Long timestampUnix,
			String timestamp,
			BigDecimal totalMinted,
			BigDecimal totalBurned,
			String circulatingSupply,
			BigDecimal validTokenUtxos,
			Long validAddresses,
			BigDecimal satoshisLockedUp,
			String documentHash,
			BigDecimal txnsSinceGenesis,
			String mintingBatonStatus,
			Integer blockLastActiveSend,
			Integer blockLastActiveMint) {
		this.decimals = decimals;
		this.documentUri = documentUri;
		this.symbol = symbol;
		this.name = name;
		this.containsBaton = containsBaton;
		this.id = id;
		this.initialTokenQty = initialTokenQty;
		this.blockCreated = blockCreated;
		this.quantity = quantity;
		this.lastActiveSend = lastActiveSend;
		this.activeMint = activeMint;
		this.versionType = Optional.ofNullable(versionType).map(Hex::decode).map(ByteUtils.INSTANCE::toInt).orElse(null);
		this.timestampUnix = timestampUnix;
		this.timestamp = timestamp;
		this.totalMinted = totalMinted.longValue();
		this.totalBurned = totalBurned.longValue();
		this.circulatingSupply = circulatingSupply;
		this.validTokenUtxos = validTokenUtxos;
		this.validAddresses = validAddresses;
		this.satoshisLockedUp = satoshisLockedUp;
		this.documentHash = documentHash;
		this.txnsSinceGenesis = txnsSinceGenesis;
		this.mintingBatonStatus = mintingBatonStatus;
		this.blockLastActiveSend = blockLastActiveSend;
		this.blockLastActiveMint = blockLastActiveMint;
	}
}
