package com.bitcoin.indexer.responses;

/* "address":"mo9ncXisMeAoXwqcV5EWuyncbmCcQN4rVs",
    "txid":"d5f8a96faccf79d4c087fa217627bb1120e83f8ea1a7d84b1de4277ead9bbac1",
    "vout":0,
    "scriptPubKey":"76a91453c0307d6851aa0ce7825ba883c6bd9ad242b48688ac",
    "amount":0.000006,
    "satoshis":600,
    "confirmations":0,
    "ts":1461349425*/

public class UtxoResponse {

	public String address;
	public String txid;
	public int vout;
	public String scriptPubKey;
	public long amount;
	public long satoshis;
	public long tokenAmount;
	public long tokenSatoshis;
	public int confirmations;
	public long ts;

	public UtxoResponse() {
	}

	public UtxoResponse(String address, String txid, int vout, String scriptPubKey, long tokenAmount, long tokenSatoshis, int confirmations, long ts,
			long amount, long satoshis) {
		this.address = address;
		this.txid = txid;
		this.vout = vout;
		this.scriptPubKey = scriptPubKey;
		this.tokenAmount = tokenAmount;
		this.tokenSatoshis = tokenSatoshis;
		this.confirmations = confirmations;
		this.ts = ts;
		this.amount = amount;
		this.satoshis = satoshis;
	}
}
