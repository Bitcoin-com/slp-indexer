package com.bitcoin.indexer;

import static org.hamcrest.MatcherAssert.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.bitcoinj.params.MainNetParams;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.spongycastle.util.encoders.Hex;

import com.bitcoin.indexer.blockchain.domain.Address;
import com.bitcoin.indexer.blockchain.domain.Input;
import com.bitcoin.indexer.blockchain.domain.NetValueCalculator;
import com.bitcoin.indexer.blockchain.domain.Transaction;
import com.bitcoin.indexer.blockchain.domain.Utxo;

public class NetValueCalculatorTest {

	//5439c37c4c6dad4ad93b1d5598d57f1aace9d0f68b892236da903b3f136451a1
	@Test
	public void netValue() {
		org.bitcoinj.core.Transaction transaction = new org.bitcoinj.core.Transaction(
				MainNetParams.get(),
				Hex.decode(
						"0100000002774328ddff50701ade3a2e1f28711643a17ad5f53f1e94639b04234fa0a5bbcf00000000494830450220456c80524bf6d7c542839791067ea98afe10e1b271422808d2117469fc33a7dd0221008beb72efed006d244ab4933bb150a7f4330f2ce850b4f77f7884e4350efc03c701ffffffff78b733ee4c2438f7379ca1211e575cce9bf4ab139e6ea01ab6c31d411e862b280000000049483045022020f0110caa1819d3338267b466b162bee8a7b955393e7a1b3ee25bc4f33b3216022100c1117a48db19917536687ebe4feea58f864b625d4ec7b506d3ef02b3ac022ac201ffffffff0100e40b54020000004341042c0960594d5e48ccc8edb8a7ac622d5b542a1058257aecbf5f2e75ae8cfffeb98ac6cd8aa268a671857053c2d094725ffa2d22c5fc7872ddb6ffc8ee298a3ec9ac00000000")
		);
		Utxo utxo = Utxo.create(
				transaction.getHashAsString(),
				Address.create("133fZZzNNbHL5VSuCWrUkLW2oL9ZPJELbY"),
				"",
				new BigDecimal("10000000000"),
				false,
				0,
				false,
				Instant.ofEpochMilli(10),
				null,
				false
		);

		Input first = Input.unknownValue(Address.create("132aSc15WmoPwtMbqRVzouZKNnjWL1YTVb"),
				0,
				"cfbba5a04f23049b63941e3ff5d57aa1431671281f2e3ade1a7050ffdd284377",
				false,
				1L);

		Input second = Input.unknownValue(Address.create("17oofDoUGPaTi7xEP3StP1sU1YxEMDfQa6"),
				0,
				"282b861e411dc3b61aa06e9e13abf49bce5c571e21a19c37f738244cee33b778",
				false,
				1L);

		Transaction tx = Transaction.create(
				transaction.getHashAsString(),
				List.of(utxo),
				List.of(first, second),
				false,
				BigDecimal.ZERO,
				Instant.ofEpochMilli(100),
				false,
				null,
				null,
				List.of(),
				null,
				"",
				1,
				1,
				1,
				Instant.ofEpochMilli(20)
		);

		BigDecimal bigDecimal = NetValueCalculator.calculateAddressTxNetValue(Address.create("133fZZzNNbHL5VSuCWrUkLW2oL9ZPJELbY"),
				tx);

		System.out.println();
		assertThat(bigDecimal, Matchers.is(new BigDecimal("10000000000")));

	}

}