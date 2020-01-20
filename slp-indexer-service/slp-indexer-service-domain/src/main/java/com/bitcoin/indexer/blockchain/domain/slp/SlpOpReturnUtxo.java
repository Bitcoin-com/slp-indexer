package com.bitcoin.indexer.blockchain.domain.slp;

import com.bitcoin.indexer.blockchain.domain.Utxo;

import java.util.Objects;

public class SlpOpReturnUtxo {

	private final Utxo utxo;
	private final SlpOpReturn slpOpReturn;

	private SlpOpReturnUtxo(Utxo utxo, SlpOpReturn slpOpReturn) {
		this.utxo = Objects.requireNonNull(utxo);
		this.slpOpReturn = Objects.requireNonNull(slpOpReturn);
	}

	public static SlpOpReturnUtxo create(Utxo utxo, SlpOpReturn slpOpReturn) {
		return new SlpOpReturnUtxo(utxo, slpOpReturn);
	}

	public Utxo getUtxo() {
		return utxo;
	}

	public SlpOpReturn getSlpOpReturn() {
		return slpOpReturn;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		SlpOpReturnUtxo that = (SlpOpReturnUtxo) o;
		return Objects.equals(utxo, that.utxo) &&
				Objects.equals(slpOpReturn, that.slpOpReturn);
	}

	@Override
	public int hashCode() {
		return Objects.hash(utxo, slpOpReturn);
	}
}
