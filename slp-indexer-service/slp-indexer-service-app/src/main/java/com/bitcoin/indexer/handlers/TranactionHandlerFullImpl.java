package com.bitcoin.indexer.handlers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bitcoin.indexer.blockchain.domain.IndexerTransaction;
import com.bitcoin.indexer.blockchain.domain.Input;
import com.bitcoin.indexer.blockchain.domain.Transaction;
import com.bitcoin.indexer.blockchain.domain.Utxo;
import com.bitcoin.indexer.repository.TransactionRepository;

import io.reactivex.Completable;
import io.reactivex.Flowable;

public class TranactionHandlerFullImpl implements TransactionHandler {

	private static final Logger logger = LoggerFactory.getLogger(TransactionHandlerSlpImpl.class);
	private final InputHandler inputHandler;
	private final UtxoHandler utxoHandler;
	private TransactionRepository transactionRepository;
	private ForkJoinPool customThreadPool = new ForkJoinPool(32);

	public TranactionHandlerFullImpl(InputHandler inputHandler,
			UtxoHandler utxoHandler,
			TransactionRepository transactionRepository) {
		this.inputHandler = Objects.requireNonNull(inputHandler);
		this.utxoHandler = Objects.requireNonNull(utxoHandler);
		this.transactionRepository = transactionRepository;
	}

	@Override
	public Flowable<IndexerTransaction> handleTransaction(List<IndexerTransaction> transaction) {

		try {
			Map<IndexerTransaction, List<Utxo>> result = new ConcurrentHashMap<>();

			customThreadPool.submit(() -> {
				try {
					result.putAll(transaction.parallelStream().collect(Collectors.toMap(tx -> tx, tx -> utxoHandler.handleUtxos(tx).blockingGet())));
				} catch (Exception e) {
					logger.info("Error handling utxos", e);
					throw new RuntimeException(e);
				}
			}).get();

			List<IndexerTransaction> txs = new ArrayList<>();

			customThreadPool.submit(() -> {
				result.entrySet().parallelStream().forEach(entry -> {
							try {
								IndexerTransaction walletTx = entry.getKey();
								List<Input> inputsWithValue = inputHandler.handleInput(walletTx).blockingGet();
								IndexerTransaction indexerTransaction = withInputValue(walletTx, inputsWithValue);
								txs.add(indexerTransaction);
							} catch (Exception e) {
								logger.info("Error handling inputs entry={}", entry.getKey().getTransaction().getTxId(), e);
								throw new RuntimeException(e);
							}
						}
				);
			}).get();

			customThreadPool.submit(() -> {
				try {
					transactionRepository.saveTransaction(txs).blockingGet();
				} catch (Exception e) {
					logger.info("Error handling txs", e);
					throw new RuntimeException(e);
				}
			}).get();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		return Flowable.fromIterable(transaction);
	}

	@Override
	public Completable reorganizeFromBlock(List<IndexerTransaction> oldTxs) {
		for (IndexerTransaction transaction : oldTxs) {
			logger.info("Handled reorg for tx={}", transaction);

			Throwable throwable = utxoHandler.handleReorg(transaction).blockingGet();

			if (throwable != null) {
				logger.error("Failed to reorg utxos transaction={}", transaction, throwable);
			}

			throwable = inputHandler.handleReorg(transaction).blockingGet();

			if (throwable != null) {
				logger.error("Failed to reorg inputs transaction={}", transaction, throwable);
			}

			//throwable = historyHandler.handleReorg(transaction).blockingGet();

			if (throwable != null) {
				logger.error("Failed to reorg history transaction={}", transaction, throwable);
			}
		}

		return Completable.complete();
	}

	private IndexerTransaction withInputValue(IndexerTransaction indexerTransaction, List<Input> inputValue) {
		return IndexerTransaction.create(
				Transaction.create(
						indexerTransaction.getTransaction().getTxId(),
						indexerTransaction.getTransaction().getOutputs(),
						inputValue,
						indexerTransaction.getTransaction().isConfirmed(),
						indexerTransaction.getTransaction().getFees(),
						indexerTransaction.getTransaction().getTime(),
						indexerTransaction.getTransaction().isFromBlock(),
						indexerTransaction.getTransaction().getBlockHash().orElse(null),
						indexerTransaction.getTransaction().getBlockHeight().orElse(null),
						indexerTransaction.getTransaction().getSlpOpReturn(),
						indexerTransaction.getTransaction().getSlpValid().orElse(null),
						indexerTransaction.getTransaction().getRawHex(),
						indexerTransaction.getTransaction().getVersion(),
						indexerTransaction.getTransaction().getLocktime(),
						indexerTransaction.getTransaction().getSize(),
						indexerTransaction.getTransaction().getBlockTime().orElse(null)
				));
	}
}
