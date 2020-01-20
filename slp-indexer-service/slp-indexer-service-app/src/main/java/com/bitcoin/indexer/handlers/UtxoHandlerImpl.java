package com.bitcoin.indexer.handlers;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bitcoin.indexer.blockchain.domain.IndexerTransaction;
import com.bitcoin.indexer.blockchain.domain.Utxo;
import com.bitcoin.indexer.core.Coin;
import com.bitcoin.indexer.repository.UtxoRepository;

import io.reactivex.Completable;
import io.reactivex.Single;

public class UtxoHandlerImpl implements UtxoHandler {

	private final UtxoRepository walletUtxoRepository;
	private Coin coin;
	private static final Logger logger = LoggerFactory.getLogger(UtxoHandlerImpl.class);

	public UtxoHandlerImpl(UtxoRepository walletUtxoRepository, Coin coin) {
		this.walletUtxoRepository = Objects.requireNonNull(walletUtxoRepository);
		this.coin = Objects.requireNonNull(coin);
	}

	@Override
	public Single<List<Utxo>> handleUtxos(IndexerTransaction indexerTransaction) {
		return walletUtxoRepository.saveUtxo(indexerTransaction.getTransaction().getOutputs()
				.stream()
				.filter(e -> e.getSlpUtxo().isPresent())
				.collect(Collectors.toList()), coin)
				.retry(1)
				.doOnError(er -> logger.error("Could not save utxo", er));
	}

	@Override
	public Completable handleReorg(IndexerTransaction indexerTransaction) {
		return walletUtxoRepository.removeUtxo(indexerTransaction.getTransaction().getOutputs(), Coin.BCH)
				.doOnError(er -> logger.error("Could not remove utxos for tx={}", indexerTransaction.getTransaction().getTxId(), er))
				.ignoreElement();
	}
}
