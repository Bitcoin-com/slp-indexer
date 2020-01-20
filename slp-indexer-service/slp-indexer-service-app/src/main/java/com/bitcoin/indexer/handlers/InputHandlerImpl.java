package com.bitcoin.indexer.handlers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bitcoin.indexer.blockchain.domain.IndexerTransaction;
import com.bitcoin.indexer.blockchain.domain.Input;
import com.bitcoin.indexer.blockchain.domain.Utxo;
import com.bitcoin.indexer.core.Coin;
import com.bitcoin.indexer.repository.UtxoRepository;

import io.reactivex.Completable;
import io.reactivex.Single;

public class InputHandlerImpl implements InputHandler {

	private final UtxoRepository walletUtxoRepository;
	private final Coin coin;

	private static final Logger logger = LoggerFactory.getLogger(InputHandlerImpl.class);

	public InputHandlerImpl(UtxoRepository walletUtxoRepository, Coin coin) {
		this.walletUtxoRepository = Objects.requireNonNull(walletUtxoRepository);
		this.coin = Objects.requireNonNull(coin);
	}

	@Override
	public Single<List<Input>> handleInput(IndexerTransaction transaction) {
		return spendUtxo(transaction.getTransaction().getInputs())
				.map(e -> getInputsWithValue(e, transaction, transaction.getTransaction().getInputs()))
				.retry(1)
				.doOnError(er -> logger.error("Could not spend", er));
	}

	@Override
	public Completable handleReorg(IndexerTransaction transaction) {
		return Completable.complete();
	}

	private Single<List<Utxo>> spendUtxo(List<Input> inputs) {
		return walletUtxoRepository.fetchUtxo(inputs, coin)
				.flatMap(utxos -> walletUtxoRepository.spendUtxo(utxos, coin));
	}

	private List<Input> getInputsWithValue(List<Utxo> utxos, IndexerTransaction transaction, List<Input> inputs) {
		Map<String, Long> sequence = inputs.stream().collect(Collectors.toMap(k -> k.getTxId() + ":" + k.getIndex(), Input::getSequence));

		List<Input> collect = utxos.stream()
				.map(e -> {
					long seq = sequence.containsKey(e.getTxId() + ":" + e.getIndex()) ? sequence.get((e.getTxId() + ":" + e.getIndex())) : 0L;
					return Input.knownValue(e.getAddress(),
							e.getAmount(),
							e.getIndex(),
							e.getTxId(),
							e.getSlpUtxo().orElse(null), false,
							seq);
				})
				.collect(Collectors.toCollection(ArrayList::new));
		transaction.getTransaction().getInputs().stream().filter(Input::isCoinbase).findFirst()
				.map(collect::add);
		return collect;
	}
}
