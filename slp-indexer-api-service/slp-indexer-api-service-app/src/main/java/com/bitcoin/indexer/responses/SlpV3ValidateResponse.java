package com.bitcoin.indexer.responses;

public class SlpV3ValidateResponse {

	public String txid;
	public String valid;
	public String reason;

	public SlpV3ValidateResponse(String txid, String valid, String reason) {
		this.txid = txid;
		this.valid = valid;
		this.reason = reason;
	}

}
