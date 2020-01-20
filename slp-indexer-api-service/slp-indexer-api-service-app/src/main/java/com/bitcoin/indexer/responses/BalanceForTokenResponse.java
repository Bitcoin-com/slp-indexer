package com.bitcoin.indexer.responses;

/*{
    "tokenBalance": 1000,
    "tokenBalanceString": "1000",
    "slpAddress": "simpleledger:qzhfd7ssy9nt4gw7j9w5e7w5mxx5w549rv7mknzqkz",
    "tokenId": "df808a41672a0a0ae6475b44f272a107bc9961b90f29dc918d71301f24fe92fb"
  },*/

import java.math.BigDecimal;

public class BalanceForTokenResponse {

	public BigDecimal tokenBalance;
	public String tokenBalanceString;
	public String slpAddress;
	public String tokenId;

	public BalanceForTokenResponse(BigDecimal tokenBalance, String tokenBalanceString, String slpAddress, String tokenId) {
		this.tokenBalance = tokenBalance;
		this.tokenBalanceString = tokenBalanceString;
		this.slpAddress = slpAddress;
		this.tokenId = tokenId;
	}

	public BalanceForTokenResponse() {
	}
}
