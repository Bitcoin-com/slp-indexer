package com.bitcoin.indexer.responses;

/*{
  "slpAddress": "simpleledger:qz9tzs6d5097ejpg279rg0rnlhz546q4fsnck9wh5m",
  "cashAddress": "bitcoincash:qz9tzs6d5097ejpg279rg0rnlhz546q4fslra7mh29",
  "legacyAddress": "1DeLbv5EMzLEFDvQ8wZiKeSuPGGtSSz5HP"
}*/
public class AddressConvertResponse {
	public String slpAddress;
	public String cashAddress;
	public String legacyAddress;

	public AddressConvertResponse(String slpAddress, String cashAddress, String legacyAddress) {
		this.slpAddress = slpAddress;
		this.cashAddress = cashAddress;
		this.legacyAddress = legacyAddress;
	}
}
