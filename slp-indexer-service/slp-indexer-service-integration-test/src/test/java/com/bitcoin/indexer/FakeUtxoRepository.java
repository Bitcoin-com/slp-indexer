package com.bitcoin.indexer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.util.Pair;

import com.bitcoin.indexer.blockchain.domain.Address;
import com.bitcoin.indexer.blockchain.domain.Input;
import com.bitcoin.indexer.blockchain.domain.Utxo;
import com.bitcoin.indexer.core.Coin;
import com.bitcoin.indexer.repository.UtxoRepository;

import io.reactivex.Maybe;
import io.reactivex.Single;

public class FakeUtxoRepository implements UtxoRepository {
	private final Map<String, Utxo> map = new HashMap<>();

	@Override
	public Single<List<Utxo>> fetchUtxosFromAddress(Address address, Coin coin, boolean useCache) {
		return null;
	}

	@Override
	public Single<List<Utxo>> fetchSlpUtxosForAddress(Address address, Coin coin, boolean useCache) {
		return null;
	}

	@Override
	public Maybe<Utxo> fetchUtxo(String txId, int inputIndex, Coin coin) {
		Utxo utxo = map.get(txId + ":" + inputIndex);
		if (utxo == null) {
			return Maybe.empty();
		}

		return Maybe.just(utxo);
	}

	@Override
	public Single<List<Utxo>> fetchUtxosWithTokenId(List<String> tokenIds, boolean isSpent) {
		return Single.just(new ArrayList<>(map.values()));
	}

	@Override
	public Single<List<Utxo>> fetchUtxos(List<Pair<String, Integer>> txIdIndexs, Coin coin) {
		return null;
	}

	@Override
	public Single<List<Utxo>> fetchUtxo(List<Input> inputs, Coin coin) {
		return null;
	}

	@Override
	public Single<List<Utxo>> fetchUtxos(String txId, Coin coin) {
		List<Utxo> utxos = map.values()
				.stream()
				.filter(utxo -> utxo.getTxId().equals(txId))
				.collect(Collectors.toList());
		return Single.just(utxos);
	}

	@Override
	public Single<List<Utxo>> saveUtxo(List<Utxo> utxos, Coin coin) {
		utxos.forEach(u -> {
			map.put(u.getTxId() + ":" + u.getIndex(), u);
		});

		return Single.just(utxos);
	}

	@Override
	public Single<List<Utxo>> spendUtxo(List<Utxo> utxos, Coin coin) {
		return null;
	}

	@Override
	public Single<List<Utxo>> removeUtxo(List<Utxo> utxos, Coin coin) {
		return null;
	}

	@Override
	public void addToCache(Utxo utxo) {

	}
}
