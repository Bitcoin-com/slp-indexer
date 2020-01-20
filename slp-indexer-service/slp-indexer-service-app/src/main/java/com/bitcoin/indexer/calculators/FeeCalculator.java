package com.bitcoin.indexer.calculators;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import com.bitcoin.indexer.blockchain.domain.Input;
import com.bitcoin.indexer.blockchain.domain.Utxo;

public class FeeCalculator {

	public static BigDecimal calculateFee(List<Input> inputs, List<Utxo> utxos) {
		BigDecimal inputValue = inputs.stream()
				.map(Input::getAmount)
				.filter(Optional::isPresent)
				.map(Optional::get)
				.reduce(BigDecimal::add).orElse(BigDecimal.ZERO);
		BigDecimal outputValue = utxos.stream()
				.map(Utxo::getAmount)
				.reduce(BigDecimal::add).orElse(BigDecimal.ZERO);
		if (inputValue.signum() == 0) {
			return BigDecimal.ZERO;
		}

		return inputValue.subtract(outputValue);
	}
}
