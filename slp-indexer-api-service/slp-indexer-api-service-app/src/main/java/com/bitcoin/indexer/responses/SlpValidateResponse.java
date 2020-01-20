package com.bitcoin.indexer.responses;

public class SlpValidateResponse {

	public String txid;
	public boolean valid;
	public String reason;

	public SlpValidateResponse(String txid, boolean valid, String reason) {
		this.txid = txid;
		this.valid = valid;
		this.reason = reason;
	}
}
