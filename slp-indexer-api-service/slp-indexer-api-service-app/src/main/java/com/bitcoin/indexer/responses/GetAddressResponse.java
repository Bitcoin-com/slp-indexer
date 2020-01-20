package com.bitcoin.indexer.responses;


/*{
  "addrStr": "1LVFmKNicQVGitPyAvurZNntGTB7GGx1zS",
  "balance": 0,
  "balanceSat": 0,
  "totalReceived": 6.48,
  "totalReceivedSat": 648000000,
  "totalSent": 6.48,
  "totalSentSat": 648000000,
  "unconfirmedBalance": 0,
  "unconfirmedBalanceSat": 0,
  "unconfirmedTxApperances": 0,
  "txApperances": 2,
  "transactions": [
    "1c27b98f5b975576ca5c9dd3dc062df39749a274e359bda82a47bf7315963598",
    "53762039d31afaa38a641ecd789e01884da7fee0dc34fef721d7ae8f58e9c6fd"
  ]
}*/

import java.math.BigDecimal;
import java.util.List;

public class GetAddressResponse {
	public String addrStr;
	public BigDecimal balance;
	public long balanceSat;
	public BigDecimal totalReceived;
	public long totalReceivedSat;
	public long unconfirmedBalance;
	public long unconfirmedBalanceSat;
	public int unconfirmedTxApperances;
	public int txApperances;
	public List<String> transactions;

	public GetAddressResponse(String addrStr,
			BigDecimal balance,
			long balanceSat,
			BigDecimal totalReceived,
			long totalReceivedSat,
			long unconfirmedBalance,
			long unconfirmedBalanceSat,
			int unconfirmedTxApperances,
			int txApperances,
			List<String> transactions) {
		this.addrStr = addrStr;
		this.balance = balance;
		this.balanceSat = balanceSat;
		this.totalReceived = totalReceived;
		this.totalReceivedSat = totalReceivedSat;
		this.unconfirmedBalance = unconfirmedBalance;
		this.unconfirmedBalanceSat = unconfirmedBalanceSat;
		this.unconfirmedTxApperances = unconfirmedTxApperances;
		this.txApperances = txApperances;
		this.transactions = transactions;
	}
}
