package com.bitcoin.indexer.blockchain.domain;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class BlockReward {

	public static BigDecimal getReward(int height) {
		return rewards.floorEntry(height).getValue();
	}

	private static final Map<Integer, BigDecimal> satoshis = new HashMap<>();

	static {
		satoshis.put(0, new BigDecimal("5000000000"));
		satoshis.put(210000, new BigDecimal("2500000000"));
		satoshis.put(420000, new BigDecimal("1250000000"));
		satoshis.put(630000, new BigDecimal("625000000"));
		satoshis.put(840000, new BigDecimal("312500000"));
		satoshis.put(1050000, new BigDecimal("156250000"));
		satoshis.put(1260000, new BigDecimal("78125000"));
		satoshis.put(1470000, new BigDecimal("39062500"));
		satoshis.put(1680000, new BigDecimal("19531250"));
		satoshis.put(1890000, new BigDecimal("9765625"));
		satoshis.put(2100000, new BigDecimal("4882812"));
		satoshis.put(2310000, new BigDecimal("2441406"));
		satoshis.put(2520000, new BigDecimal("1220703"));
		satoshis.put(2730000, new BigDecimal("610351"));
		satoshis.put(2940000, new BigDecimal("305175"));
		satoshis.put(3150000, new BigDecimal("152587"));
		satoshis.put(3360000, new BigDecimal("76293"));
		satoshis.put(3570000, new BigDecimal("38146"));
		satoshis.put(3780000, new BigDecimal("19073"));
		satoshis.put(3990000, new BigDecimal("9536"));
		satoshis.put(4200000, new BigDecimal("4768"));
		satoshis.put(4410000, new BigDecimal("2384"));
		satoshis.put(4620000, new BigDecimal("1192"));
		satoshis.put(4830000, new BigDecimal("596"));
		satoshis.put(5040000, new BigDecimal("298"));
		satoshis.put(5250000, new BigDecimal("149"));
		satoshis.put(5460000, new BigDecimal("74"));
		satoshis.put(5670000, new BigDecimal("37"));
		satoshis.put(5880000, new BigDecimal("18"));
		satoshis.put(6090000, new BigDecimal("9"));
		satoshis.put(6300000, new BigDecimal("4"));
		satoshis.put(6510000, new BigDecimal("2"));
		satoshis.put(6720000, new BigDecimal("1"));
		satoshis.put(6930000, new BigDecimal("0"));
	}

	private static final NavigableMap<Integer, BigDecimal> rewards = satoshis.entrySet()
			.stream()
			.collect(Collectors.toMap(Entry::getKey, Entry::getValue,
					(v1, v2) -> {
						throw new RuntimeException("Duplicate key " + v1);
					},
					TreeMap::new));

}