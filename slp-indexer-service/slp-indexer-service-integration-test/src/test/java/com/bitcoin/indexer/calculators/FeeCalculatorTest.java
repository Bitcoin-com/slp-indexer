package com.bitcoin.indexer.calculators;

import static org.hamcrest.MatcherAssert.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

import org.hamcrest.Matchers;
import org.junit.Test;

import com.bitcoin.indexer.blockchain.domain.Address;
import com.bitcoin.indexer.blockchain.domain.Input;
import com.bitcoin.indexer.blockchain.domain.Utxo;

public class FeeCalculatorTest {

	@Test
	public void calculate_fee() {
		List<Input> inputs = Collections.singletonList(Input.knownValue(Address.create(""), new BigDecimal("100"), 0,
				"123", null, false, 0L));

		List<Utxo> utxos = Collections.singletonList(Utxo.unconfirmed("12333", Address.create("44"), "123",
				new BigDecimal("90"), Instant.ofEpochMilli(100), 0, false));

		BigDecimal fee = FeeCalculator.calculateFee(inputs, utxos);

		assertThat(fee, Matchers.is(BigDecimal.TEN));
	}
}
