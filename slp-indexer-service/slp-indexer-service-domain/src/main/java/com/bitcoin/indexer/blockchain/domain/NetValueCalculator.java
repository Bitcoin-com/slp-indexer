package com.bitcoin.indexer.blockchain.domain;

import java.math.BigDecimal;

public class NetValueCalculator {

	public static BigDecimal calculateAddressTxNetValue(Address address, Transaction transaction) {
		BigDecimal inputValue = transaction.getInputs()
				.stream()
				.filter(e -> e.getAddress().equals(address))
				.map(e -> e.getAmount().orElse(BigDecimal.ZERO))
				.reduce(BigDecimal::add).orElse(BigDecimal.ZERO);

		BigDecimal outputValue = transaction.getOutputs()
				.stream()
				.filter(e -> e.getAddress().equals(address))
				.map(Utxo::getAmount)
				.reduce(BigDecimal::add).orElse(BigDecimal.ZERO);

		return outputValue.subtract(inputValue);
	}
}
