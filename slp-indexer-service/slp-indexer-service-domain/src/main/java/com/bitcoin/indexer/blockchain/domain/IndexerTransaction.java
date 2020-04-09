package com.bitcoin.indexer.blockchain.domain;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import com.bitcoin.indexer.blockchain.domain.slp.SlpUtxo;
import com.bitcoin.indexer.blockchain.domain.slp.SlpValid;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

public class IndexerTransaction {

	private final Transaction transaction;
	private Multimap<Address, TransactionData> concernedAddresses;

	private IndexerTransaction(Transaction transaction, Multimap<Address, TransactionData> concernedAddresses) {
		this.transaction = Objects.requireNonNull(transaction);
		this.concernedAddresses = HashMultimap.create(concernedAddresses);
	}

	public static IndexerTransaction create(Transaction transaction) {
		Multimap<Address, TransactionData> result = HashMultimap.create();
		for (Utxo output : transaction.getOutputs()) {
			result.put(output.getAddress(), new TransactionData(transaction, output.getAddress(), output, null));
		}

		for (Input input : transaction.getInputs()) {
			result.put(input.getAddress(), new TransactionData(transaction, input.getAddress(), null, input));
		}

		return new IndexerTransaction(transaction, result);
	}

	public IndexerTransaction withValid(SlpValid slpValid) {
		List<Utxo> updatedUtxos = this.getTransaction().getOutputs().stream()
				.map(e -> e.withValid(slpValid)).collect(Collectors.toList());

		return IndexerTransaction.create(
				Transaction.create(
						this.getTransaction().getTxId(),
						updatedUtxos,
						this.getTransaction().getInputs(),
						this.getTransaction().isConfirmed(),
						this.getTransaction().getFees(),
						this.getTransaction().getTime(),
						this.getTransaction().isFromBlock(),
						this.getTransaction().getBlockHash().orElse(null),
						this.getTransaction().getBlockHeight().orElse(null),
						this.getTransaction().getSlpOpReturn(),
						slpValid,
						this.getTransaction().getRawHex(),
						this.getTransaction().getVersion(),
						this.getTransaction().getLocktime(),
						this.getTransaction().getSize(),
						this.getTransaction().getBlockTime().orElse(null)
				));
	}

	public Transaction getTransaction() {
		return transaction;
	}

	public boolean isSlp() {
		return !transaction.getSlpOpReturn().isEmpty();
	}

	public boolean hasGenesisOutput() {
		return transaction.getOutputs().stream()
				.filter(e -> e.getSlpUtxo().isPresent())
				.map(e -> e.getSlpUtxo().get())
				.anyMatch(SlpUtxo::isGenesis);
	}

	public Multimap<Address, TransactionData> getConcernedAddresses() {
		return concernedAddresses;
	}

	public static class TransactionData {
		private Transaction transaction;
		private Address address;
		private Utxo utxo;
		private Input input;

		private TransactionData(Transaction transaction, Address address, Utxo utxo, Input input) {
			this.transaction = transaction;
			this.address = address;
			this.utxo = utxo;
			this.input = input;
		}

		public Transaction getTransaction() {
			return transaction;
		}

		public Address getAddress() {
			return address;
		}

		public Optional<Utxo> getUtxo() {
			return Optional.ofNullable(utxo);
		}

		public Optional<Input> getInput() {
			return Optional.ofNullable(input);
		}
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		IndexerTransaction that = (IndexerTransaction) o;
		return Objects.equals(transaction.getTxId(), that.transaction.getTxId());
	}

	@Override
	public int hashCode() {
		return Objects.hash(transaction.getTxId());
	}

	@Override
	public String toString() {
		return "IndexerTransaction [" +
				"transaction=" + transaction +
				", concernedAddresses=" + concernedAddresses +
				']';
	}
}
