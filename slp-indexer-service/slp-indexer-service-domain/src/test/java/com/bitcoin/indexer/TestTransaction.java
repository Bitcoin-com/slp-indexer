package com.bitcoin.indexer;

public class TestTransaction {

	/*
	public static Transaction twoUtxos() {
		String txId = UUID.randomUUID().toString();
		Address address = Address.create(UUID.randomUUID().toString());
		Utxo unconfirmed = Utxo.unconfirmed(txId, address, "", BigDecimal.TEN, 0);
		Utxo second = Utxo.unconfirmed(txId, address, "", BigDecimal.TEN, 2);
		List<Utxo> utxos = Arrays.asList(unconfirmed, second);
		List<Input> inputs = Collections.singletonList(Input.knownValue(Address.create(UUID.randomUUID().toString()),
				BigDecimal.TEN, 1, UUID.randomUUID().toString(), BigDecimal.ZERO, null));
		return Transaction.fromMempool(txId, utxos, inputs, BigDecimal.TEN, Instant.ofEpochMilli(2000), Collections.emptyList(), null, "");
	}*/
}
