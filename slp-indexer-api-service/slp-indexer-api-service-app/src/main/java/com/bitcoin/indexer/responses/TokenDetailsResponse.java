package com.bitcoin.indexer.responses;

/*{
  "decimals": 2,
  "timestamp": "2018-10-11 00:39:22",
  "versionType": 1,
  "documentUri": "broccoli.cash",
  "symbol": "BROC",
  "name": "Broccoli",
  "containsBaton": false,
  "id": "259908ae44f46ef585edef4bcc1e50dc06e4c391ac4be929fae27235b8158cf1",
  "documentHash": null,
  "initialTokenQty": 1000,
  "blockCreated": 551647,
  "blockLastActiveSend": 583018,
  "blockLastActiveMint": null,
  "txnsSinceGenesis": 13,
  "validAddresses": 7,
  "totalMinted": 1000,
  "totalBurned": 980,
  "circulatingSupply": 20,
  "mintingBatonStatus": "NEVER_CREATED",
  "timestampUnix": 1539218362
}*/

public class TokenDetailsResponse {
	public int decimals;
	public String documentUri;
	public String symbol;
	public String name;
	public boolean containsBaton;
	public String id;
	public int initialTokenQty;
	public long blockCreated;

	public TokenDetailsResponse() {
	}

	public TokenDetailsResponse(int decimals, String documentUri, String symbol, String name, boolean containsBaton, String id, int initialTokenQty, long blockCreated) {
		this.decimals = decimals;
		this.documentUri = documentUri;
		this.symbol = symbol;
		this.name = name;
		this.containsBaton = containsBaton;
		this.id = id;
		this.initialTokenQty = initialTokenQty;
		this.blockCreated = blockCreated;
	}
}
