package com.bitcoin.indexer.responses;

import java.util.List;

public class Detail {
	public int decimals;
	public String tokenIdHex;
	public String transactionType;
	public Integer versionType;

	public String documentUri;
	public String documentSha256Hex;
	public String symbol;
	public String name;
	public Boolean txnBatonVout;
	public boolean txnContainsBaton;
	public List<Output> outputs;

	public Detail(int decimals,
			String tokenIdHex,
			String transactionType,
			Integer versionType,
			String documentUri,
			String documentSha256Hex,
			String symbol,
			String name,
			Boolean txnBatonVout,
			boolean txnContainsBaton,
			List<Output> outputs) {
		this.decimals = decimals;
		this.tokenIdHex = tokenIdHex;
		this.transactionType = transactionType;
		this.versionType = versionType;
		this.documentUri = documentUri;
		this.documentSha256Hex = documentSha256Hex;
		this.symbol = symbol;
		this.name = name;
		this.txnBatonVout = txnBatonVout;
		this.txnContainsBaton = txnContainsBaton;
		this.outputs = outputs;
	}


}
