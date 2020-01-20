package com.bitcoin.indexer.responses;

import java.util.List;

import com.bitcoin.indexer.facade.InsightsResponse;

public class TxDetailsResponse {

	public InsightsResponse retData;
	public TokenInfo tokenInfo;

	public TxDetailsResponse(InsightsResponse retData, TokenInfo tokenInfo) {
		this.retData = retData;
		this.tokenInfo = tokenInfo;
	}

	public TxDetailsResponse() {
	}

	public static class TokenInfo {
		public String transactionType;
		public Integer versionType;
		public String tokenIdHex;
		public List<String> sendOutputs;
		public Boolean tokenIsValid;

		public TokenInfo(String transactionType, Integer versionType, String tokenIdHex, List<String> sendOutputs, Boolean tokenIsValid) {
			this.transactionType = transactionType;
			this.versionType = versionType;
			this.tokenIdHex = tokenIdHex;
			this.sendOutputs = sendOutputs;
			this.tokenIsValid = tokenIsValid;
		}
	}
}
