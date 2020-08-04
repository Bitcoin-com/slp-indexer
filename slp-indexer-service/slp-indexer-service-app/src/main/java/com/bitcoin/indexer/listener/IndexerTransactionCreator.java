package com.bitcoin.indexer.listener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bitcoin.indexer.blockchain.domain.IndexerTransaction;
import com.bitcoin.indexer.blockchain.domain.Input;
import com.bitcoin.indexer.blockchain.domain.Transaction;
import com.bitcoin.indexer.blockchain.domain.Utxo;
import com.bitcoin.indexer.config.SystemTimer;
import com.bitcoin.indexer.core.Coin;
import com.bitcoin.indexer.repository.UtxoRepository;

public class IndexerTransactionCreator {

	private static final Logger logger = LoggerFactory.getLogger(IndexerTransactionCreator.class);
	private final UtxoRepository utxoRepository;
	private final Coin coin;

	public IndexerTransactionCreator(UtxoRepository utxoRepository, Coin coin) {
		this.utxoRepository = Objects.requireNonNull(utxoRepository);
		this.coin = Objects.requireNonNull(coin);
	}

	// Had to go back to the slow version to ensure nothing is missed :(
	public List<IndexerTransaction> createIndexerTransactionSlow(List<Transaction> transactions) {
		Map<String, Utxo> currentUtxos = transactions.stream()
				.map(t -> t.getOutputs().stream().collect(Collectors.toMap(k -> k.getTxId() + ":" + k.getIndex(), v -> v)))
				.collect(HashMap::new, Map::putAll, Map::putAll);
		return transactions.stream()
				.map(t -> createIndexerTransaction(t, currentUtxos))
				.filter(Objects::nonNull)
				.collect(Collectors.toList());
	}

	private IndexerTransaction createIndexerTransaction(Transaction transaction, Map<String, Utxo> currentUtxos) {
		//Filter out SLP only txs
		if (!transaction.isSlp()) {
			List<Input> inputs = transaction.getInputs();
			List<Utxo> utxos = utxoRepository.fetchUtxoNoCache(inputs, coin).blockingGet();
			if (!utxos.isEmpty()) {
				return IndexerTransaction.create(transaction);
			}
			for (Input input : inputs) {
				String key = input.getTxId() + ":" + input.getIndex();
				if (currentUtxos.containsKey(key)) { // This means the utxo for this input exists in the same block and should be investigated
					Utxo utxo = currentUtxos.get(key);
					if (utxo.getSlpUtxo().isPresent()) {
						// When current is not an slp and an utxo which is slp is in the same block we should index this tx
						// Prev tx is an slp in the same block and should be handled
						return IndexerTransaction.create(transaction);
					}
				}
			}
			return null;
		}
		return IndexerTransaction.create(transaction);
	}

	public List<IndexerTransaction> createIndexerTransaction(List<Transaction> transactions) {
		List<Input> inputs = transactions.stream()
				.filter(e -> !e.isSlp())
				.flatMap(e -> e.getInputs().stream()).collect(Collectors.toList());

		SystemTimer systemTimer = SystemTimer.create();
		systemTimer.start();
		List<Utxo> utxoList = List.of();
		if (!inputs.isEmpty()) {
			utxoList = utxoRepository.fetchUtxo(inputs, coin).blockingGet();
			logger.trace("Completed utxos={} fetch={}", utxoList.size(), systemTimer.getMsSinceStart());
		}

		List<IndexerTransaction> confirmedSlps = transactions.stream()
				.filter(Transaction::isSlp)
				.map(IndexerTransaction::create)
				.collect(Collectors.toList());
		if (utxoList.isEmpty() && confirmedSlps.isEmpty()) {
			return List.of();
		}

		Map<String, Utxo> txIdIndexToUtxo = utxoList.stream().collect(Collectors.toMap(e -> getKey(e.getTxId(), e.getIndex()), v -> v));
		Map<String, Input> inputsWithValue = inputs.stream()
				.filter(e -> txIdIndexToUtxo.containsKey(getKey(e.getTxId(), e.getIndex())))
				.map(e -> {
					Utxo utxo = txIdIndexToUtxo.get(getKey(e.getTxId(), e.getIndex()));
					return Input.knownValue(
							e.getAddress(),
							utxo.getAmount(),
							e.getIndex(),
							e.getTxId(),
							utxo.getSlpUtxo().orElse(null),
							e.isCoinbase(),
							e.getSequence()
					);
				}).collect(Collectors.toMap(k -> getKey(k.getTxId(), k.getIndex()), v -> v));

		List<IndexerTransaction> mightIncludeSlp = transactions.stream()
				.filter(transaction -> transaction.getInputs().stream().anyMatch(i -> inputsWithValue.containsKey(getKey(i.getTxId(), i.getIndex()))))
				.map(transaction -> {
					List<Input> valueInputs = getInputsWithValue(inputsWithValue, transaction);
					return IndexerTransaction.create(Transaction.create(
							transaction.getTxId(),
							transaction.getOutputs(),
							valueInputs,
							transaction.isConfirmed(),
							transaction.getFees(),
							transaction.getTime(),
							transaction.isFromBlock(),
							transaction.getBlockHash().orElse(null),
							transaction.getBlockHeight().orElse(null),
							transaction.getSlpOpReturn(),
							transaction.getSlpValid().orElse(null),
							transaction.getRawHex(),
							transaction.getVersion(),
							transaction.getLocktime(),
							transaction.getSize(),
							transaction.getBlockTime().orElse(null))
					);
				}).collect(Collectors.toList());
		confirmedSlps.addAll(mightIncludeSlp);
		return confirmedSlps;
	}

	private List<Input> getInputsWithValue(Map<String, Input> inputsWithValue, Transaction transaction) {
		return transaction.getInputs()
				.stream()
				.map(input -> {
					if (inputsWithValue.containsKey(getKey(input.getTxId(), input.getIndex()))) {
						return inputsWithValue.get(getKey(input.getTxId(), input.getIndex()));
					}
					return input;
				}).collect(Collectors.toList());
	}

	private String getKey(String txId, int index) {
		return txId + ":" + index;
	}
}
