package com.bitcoin.indexer.responses;

/*{
    "txid": "27e27170b546f05b2af69d6eddff8834038facf5d81302e9e562df09a5c4445f",
    "tokenDetails": {
      "valid": true,
      "detail": {
        "decimals": 2,
        "tokenIdHex": "495322b37d6b2eae81f045eda612b95870a0c2b6069c58f70cf8ef4e6a9fd43a",
        "transactionType": "SEND",
        "versionType": 1,
        "documentUri": "info@simpleledger.io",
        "documentSha256Hex": null,
        "symbol": "SLPJS",
        "name": "Awesome SLPJS README Token",
        "txnBatonVout": null,
        "txnContainsBaton": false,
        "outputs": [
          {
            "address": "simpleledger:qzejshtfw82gthydl8xm9sesexfdzya6qgf25y5vly",
            "amount": "25"
          },
          {
            "address": "simpleledger:qq5qr7hvpjxjlct00ye7m2swejnc2k9enuaedc5y69",
            "amount": "77"
          }
        ]
      },
      "invalidReason": null,
      "schema_version": 73
    }
  }*/

public class TransactionTokenAddress {
	public String txid;
	public Details tokenDetails;
	public String invalidReason;
	public int schema_version = 73;

	public TransactionTokenAddress(String txid, Details tokenDetails, String invalidReason, int schema_version) {
		this.txid = txid;
		this.tokenDetails = tokenDetails;
		this.invalidReason = invalidReason;
		this.schema_version = schema_version;
	}


}

