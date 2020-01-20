package com.bitcoin.indexer.responses;

import java.util.List;

public class RecentTransactionsResponse {


	public List<TokenTransactionResponse> transactionResponses;
	public int page;

	public RecentTransactionsResponse(List<TokenTransactionResponse> transactionResponses, int page) {
		this.transactionResponses = transactionResponses;
		this.page = page;
	}
}
