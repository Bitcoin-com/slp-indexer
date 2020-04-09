package com.bitcoin.indexer.responses;

/*{
    "tokenId": "7f8889682d57369ed0e32336f8b7e0ffec625a35cca183f4e81fde4e71a538a1",
    "balance": 518628,
    "balanceString": "518628",
    "slpAddress": "simpleledger:qz9tzs6d5097ejpg279rg0rnlhz546q4fsnck9wh5m",
    "decimalCount": 0
  }*/

import java.math.BigDecimal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bitcoin.indexer.blockchain.domain.Address;

public class BalanceResponse {

	public String tokenId;
	public BigDecimal balance;
	public String balanceString;
	public String slpAddress;
	public String cashAddress;
	public String legacyAddress;
	public Integer decimalCount;
	private static final Logger logger = LoggerFactory.getLogger(BalanceResponse.class);

	public BalanceResponse(String tokenId, BigDecimal balance, String balanceString, String slpAddress, Integer decimalCount) {
		this.tokenId = tokenId;
		this.balance = balance;
		this.balanceString = balanceString;
		this.slpAddress = slpAddress;
		try {
			if (!slpAddress.contains("simpleledger")) {
				String withPrefix = "simpleledger:" + slpAddress.trim();
				this.legacyAddress = Address.toBase58(withPrefix).getAddress();
				this.cashAddress = Address.slpToBase58(withPrefix).getAddress();
			} else {
				this.legacyAddress = Address.toBase58(slpAddress).getAddress();
				this.cashAddress = Address.slpToBase58(slpAddress).getAddress();
			}
		} catch (Exception e) {
			logger.error("Could not convert address={}", slpAddress);
		}
		this.decimalCount = decimalCount;
	}
}
