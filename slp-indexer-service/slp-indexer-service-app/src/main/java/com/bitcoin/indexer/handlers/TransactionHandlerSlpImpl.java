package com.bitcoin.indexer.handlers;

import java.util.ArrayList;
import java.util.Collections;
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
import com.bitcoin.indexer.blockchain.domain.slp.SlpValid;
import com.bitcoin.indexer.facade.validators.SlpValidatorFacade;
import com.bitcoin.indexer.repository.TransactionRepository;

import io.reactivex.Completable;
import io.reactivex.Flowable;

public class TransactionHandlerSlpImpl implements TransactionHandler {

	private static final Logger logger = LoggerFactory.getLogger(TransactionHandlerSlpImpl.class);
	private final InputHandler inputHandler;
	private final UtxoHandler utxoHandler;
	private TransactionRepository transactionRepository;
	private SlpValidatorFacade slpValidatorFacade;
	private ForkJoinPool customThreadPool = new ForkJoinPool(32);

	public TransactionHandlerSlpImpl(InputHandler inputHandler,
			UtxoHandler utxoHandler,
			TransactionRepository transactionRepository,
			SlpValidatorFacade slpValidatorFacade) {
		this.inputHandler = Objects.requireNonNull(inputHandler);
		this.utxoHandler = Objects.requireNonNull(utxoHandler);
		this.transactionRepository = transactionRepository;
		this.slpValidatorFacade = Objects.requireNonNull(slpValidatorFacade);
	}

	@Override
	public Flowable<IndexerTransaction> handleTransaction(List<IndexerTransaction> transaction) {
		logger.info("Handling txs id={}", transaction.stream().map(e -> e.getTransaction().getTxId()).collect(Collectors.joining(" : ")));
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

			List<IndexerTransaction> txs = Collections.synchronizedList(new ArrayList<>());

			customThreadPool.submit(() -> {
				result.entrySet().parallelStream().forEach(entry -> {
							try {
								IndexerTransaction walletTx = entry.getKey();
								Map<String, Input> inputsWithValue = inputHandler.handleInput(walletTx).blockingGet()
										.stream().collect(Collectors.toMap(k -> k.getTxId() + ":" + k.getIndex(), v -> v));
								IndexerTransaction indexerTransaction = withInputValue(walletTx, inputsWithValue);
								txs.add(indexerTransaction);
							} catch (Exception e) {
								logger.info("Error handling inputs entry={}", entry.getKey().getTransaction(), e);
								throw new RuntimeException(e);
							}
						}
				);
			}).get();

			//Save all txs as UNKNOWN first
			transactionRepository.saveTransaction(txs).blockingGet();

			try {
				//Validate txs
				for (IndexerTransaction indexerTransaction : txs) {
					IndexerTransaction valid = withValid(indexerTransaction);
					transactionRepository.saveTransaction(List.of(valid)).blockingGet();
				}
			} catch (Exception e) {
				logger.error("Could not validate", e);
				throw new RuntimeException(e);
			}
		} catch (Exception e) {
			logger.info("Error handling txs", e);
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
		}

		Throwable throwable = transactionRepository.handleReorg(oldTxs).blockingGet();

		if (throwable != null) {
			logger.error("Failed to reorg history transaction={}", oldTxs.stream().map(e -> e.getTransaction().getTxId()).collect(Collectors.joining(":")), throwable);
		}

		return Completable.complete();
	}

	//Validate our current tx
	private IndexerTransaction withValid(IndexerTransaction indexerTransaction) {
		if (indexerTransaction.getTransaction().getSlpOpReturn().isEmpty()) {
			return indexerTransaction;
		}

		SlpValid valid = slpValidatorFacade.isValid(indexerTransaction.getTransaction().getTxId(),
				indexerTransaction.getTransaction().getSlpOpReturn().get(0).getTokenId().getHex(),
				indexerTransaction.getTransaction().getSlpOpReturn().get(0).getTokenType().getType(),
				indexerTransaction.getTransaction().getOutputs(),
				indexerTransaction.getTransaction().getInputs(),
				slpValidatorFacade);

		return indexerTransaction.withValid(valid);
	}

	private IndexerTransaction withInputValue(IndexerTransaction indexerTransaction, Map<String, Input> inputValue) {
		List<Input> inputs = indexerTransaction.getTransaction().getInputs()
				.stream().map(i -> {
					if (inputValue.containsKey(i.getTxId() + ":" + i.getIndex())) {
						return inputValue.get(i.getTxId() + ":" + i.getIndex());
					}
					return i;
				}).collect(Collectors.toList());

		return IndexerTransaction.create(
				Transaction.create(
						indexerTransaction.getTransaction().getTxId(),
						indexerTransaction.getTransaction().getOutputs(),
						inputs,
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
