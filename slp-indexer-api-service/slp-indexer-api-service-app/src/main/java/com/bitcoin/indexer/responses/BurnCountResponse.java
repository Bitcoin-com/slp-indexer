package com.bitcoin.indexer.responses;

/*{
  "transactionId": "c7078a6c7400518a513a0bde1f4158cf740d08d3b5bfb19aa7b6657e2f4160de",
  "inputTotal": 100000100,
  "outputTotal": 100000100,
  "burnTotal": 0
}*/

import java.math.BigDecimal;

public class BurnCountResponse {

	public String transactionId;
	public BigDecimal inputTotal;
	public BigDecimal outputTotal;
	public BigDecimal burnTotal;

	public BurnCountResponse(String transactionId, BigDecimal inputTotal, BigDecimal outputTotal, BigDecimal burnTotal) {
		this.transactionId = transactionId;
		this.inputTotal = inputTotal;
		this.outputTotal = outputTotal;
		this.burnTotal = burnTotal;
	}

	public BurnCountResponse() {
	}
}
