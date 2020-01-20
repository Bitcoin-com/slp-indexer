package com.bitcoin.indexer.blockchain.domain;

import static org.junit.Assert.*;

import org.hamcrest.Matchers;
import org.junit.Test;

public class AddressTest {

	@Test
	public void convert_base58_slp() {
		Address address = Address.base58ToSlp("1QELAy6EX1mbVNAAgURbofQhMoVtL6osZe");
		assertThat(address.getAddress(), Matchers.is("simpleledger:qrldqn0tamehtvvzu62gwlazydsmxlzgfc046ksadc"));
	}

}