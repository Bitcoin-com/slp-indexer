package com.bitcoin.indexer.facade.validators;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bitcoin.indexer.blockchain.domain.IndexerTransaction;
import com.bitcoin.indexer.blockchain.domain.Input;
import com.bitcoin.indexer.blockchain.domain.Utxo;
import com.bitcoin.indexer.blockchain.domain.slp.SlpValid;
import com.bitcoin.indexer.blockchain.domain.slp.SlpValid.Valid;
import com.bitcoin.indexer.repository.TransactionRepository;
import com.bitcoin.indexer.core.Coin;

public class SlpValidatorCustomImplAssumeParentValid implements SlpValidatorFacade {

	private TransactionRepository transactionRepository;
	private final MintValidatorAssumeParentValid mintValidator;
	private final SendValidatorAssumeParentValid sendValidator;
	private final GenesisValidatorAssumeParentValid genesisValidator;
	private static final Logger logger = LoggerFactory.getLogger(SlpValidatorCustomImplAssumeParentValid.class);

	public SlpValidatorCustomImplAssumeParentValid(TransactionRepository transactionRepository,
			MintValidatorAssumeParentValid mintValidator, SendValidatorAssumeParentValid sendValidator, GenesisValidatorAssumeParentValid genesisValidator) {
		this.transactionRepository = transactionRepository;
		this.mintValidator = mintValidator;
		this.sendValidator = sendValidator;
		this.genesisValidator = genesisValidator;
	}

	@Override
	public SlpValid isValid(String txId, String tokenId, String tokenType, List<Utxo> utxos, List<Input> inputs, SlpValidatorFacade baseValidator) {
		if (tokenType == null || tokenId == null) {
			return SlpValid.invalid("tokenType, tokenId is null");
		}
		if (!utxos.get(0).isOpReturn()) {
			return SlpValid.invalid("no op_return txId=" + txId);
		}

		//Locate gensis output
		if (utxos.stream().filter(e -> e.getSlpUtxo().isPresent())
				.map(e -> e.getSlpUtxo().get())
				.anyMatch(e -> e.getTokenTransactionType().equals("GENESIS"))) {
			SlpValid valid = genesisValidator.isValid(txId, tokenId, tokenType, utxos, inputs, this);
			if (valid.getValid() == Valid.INVALID) {
				revalidateChild(txId, valid, tokenId, tokenType, baseValidator);
			}
			return valid;
		}

		//Locate mint output
		if (utxos.stream().filter(e -> e.getSlpUtxo().isPresent())
				.map(e -> e.getSlpUtxo().get())
				.anyMatch(e -> e.getTokenTransactionType().equals("MINT"))) {
			SlpValid valid = mintValidator.isValid(txId, tokenId, tokenType, utxos, inputs, this);
			if (valid.getValid() == Valid.INVALID) {
				revalidateChild(txId, valid, tokenId, tokenType, baseValidator);
			}
			return valid;
		}

		//Locate send output
 		SlpValid valid = sendValidator.isValid(txId, tokenId, tokenType, utxos, inputs, this);
		if (valid.getValid() == Valid.INVALID) {
			revalidateChild(txId, valid, tokenId, tokenType, baseValidator);
		}

		return valid;
	}

	private void revalidateChild(String txId, SlpValid slpValid, String tokenId, String tokenType, SlpValidatorFacade baseValidator) {
		IndexerTransaction indexerTransaction = transactionRepository.fetchTransaction(txId, Coin.BCH, true).blockingGet();
		if (indexerTransaction == null) {
			return;
		}

		IndexerTransaction withValid = indexerTransaction.withValid(slpValid);
		transactionRepository.saveTransaction(List.of(withValid)).blockingGet();
		revalidateChild(withValid, tokenId, tokenType, baseValidator);
	}

	private void revalidateChild(IndexerTransaction indexerTransaction, String tokenId, String tokenType, SlpValidatorFacade baseValidator) {
		Set<IndexerTransaction> childs = indexerTransaction.getTransaction().getOutputs().stream().map(Utxo::getTxId)
				.map(e -> transactionRepository.fetchTransaction(e, Coin.BCH, true).blockingGet())
				.filter(Objects::nonNull)
				.filter(e -> e.getTransaction().getSlpValid().isPresent())
				.filter(e -> e.getTransaction().getSlpValid().get().getValid() == Valid.UNKNOWN || e.getTransaction().getSlpValid().get().getValid() == Valid.VALID)
				.collect(Collectors.toSet());

		if (!childs.isEmpty()) {
			for (IndexerTransaction child : childs) {
				SlpValid valid = baseValidator.isValid(child.getTransaction().getTxId(), tokenId, tokenType, child.getTransaction().getOutputs(), child.getTransaction().getInputs(), baseValidator);
				IndexerTransaction newValid = child.withValid(valid);
				transactionRepository.saveTransaction(List.of(newValid));
				revalidateChild(child, tokenId, tokenType, baseValidator);
			}
		}
	}

}
