package com.bitcoin.indexer.facade.validators;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bitcoin.indexer.blockchain.domain.IndexerTransaction;
import com.bitcoin.indexer.blockchain.domain.Input;
import com.bitcoin.indexer.blockchain.domain.Utxo;
import com.bitcoin.indexer.blockchain.domain.slp.SlpTokenType;
import com.bitcoin.indexer.blockchain.domain.slp.SlpUtxo;
import com.bitcoin.indexer.blockchain.domain.slp.SlpValid;
import com.bitcoin.indexer.blockchain.domain.slp.SlpValid.Valid;
import com.bitcoin.indexer.core.Coin;
import com.bitcoin.indexer.repository.TransactionRepository;
import com.bitcoin.indexer.repository.UtxoRepository;

public class SendValidatorAssumeParentValid implements SlpValidatorFacade {
	private TransactionRepository transactionRepository;
	private UtxoRepository utxoRepository;
	private static final Logger logger = LoggerFactory.getLogger(SendValidatorAssumeParentValid.class);

	public SendValidatorAssumeParentValid(TransactionRepository transactionRepository, UtxoRepository utxoRepository) {
		this.transactionRepository = transactionRepository;
		this.utxoRepository = utxoRepository;
	}

	@Override
	public SlpValid isValid(String txId, String tokenId, String tokenType, List<Utxo> utxos, List<Input> inputs, SlpValidatorFacade baseValidator) {
		BigDecimal currentTransactionTotalSlpTokenValue = utxos.stream().filter(e -> e.getSlpUtxo().isPresent())
				.map(e -> e.getSlpUtxo().get().getAmount())
				.reduce(BigDecimal::add).orElse(BigDecimal.ZERO);

		BigDecimal totalPreviousTxTokenValue = BigDecimal.ZERO;

		List<String> prevTxs = new ArrayList<>();

		IndexerTransaction currentTx = transactionRepository.fetchTransaction(txId, Coin.BCH, true).blockingGet();
		if (currentTx != null) {
			if (currentTx.getTransaction().getSlpValid().isPresent()) {
				SlpValid slpValid = currentTx.getTransaction().getSlpValid().get();
				if (slpValid.getValid() != Valid.UNKNOWN) {
					return slpValid;
				}
			}
		}

		for (Input input : inputs) {
			IndexerTransaction prevTx = transactionRepository.fetchTransaction(input.getTxId(), Coin.BCH, true).blockingGet();
			if (prevTx == null) {
				//We assume that we already have saved all previous SLP txs that are needed for this tx.
				//If we don't have the prevTx we assume this tx is a normal BCH transaction which we don't track
				//So let's continue and see if there is another SLP in our inputs
				continue;
			}

			//Prev tx doesn't seem to have an output that has our tokenId. So let's skip it and treat it as a normal BCH tx
			if (prevTx.getTransaction().getOutputs().stream().filter(utxo -> utxo.getSlpUtxo().isPresent())
					.map(utxo -> utxo.getSlpUtxo().get())
					.noneMatch(slpUtxo -> slpUtxo.getSlpTokenId().getHex().equals(tokenId))) {
				continue;
			}

			prevTxs.add(prevTx.getTransaction().getTxId());

			if (prevTx.getTransaction().getOutputs().stream().filter(utxo -> utxo.getSlpUtxo().isPresent())
					.map(utxo -> utxo.getSlpUtxo().get())
					.noneMatch(slpUtxo -> slpUtxo.getTokenType().equals(tokenType))) {
				return SlpValid.invalid(
						"None of prevTx utxos matches this tokenType prevTx=" + prevTx.getTransaction().getTxId() +
								" txId=" + txId +
								"tokenType=" + tokenType);
			}

			Valid prevTxValid = prevTx.getTransaction().getSlpValid().orElse(SlpValid.unknown()).getValid();
			if (prevTxValid == Valid.VALID || prevTxValid == Valid.UNKNOWN) {
				totalPreviousTxTokenValue = totalPreviousTxTokenValue.add(prevTx.getTransaction().getOutputs()
						.get(input.getIndex())
						.getSlpUtxo()
						.filter(e -> e.getSlpTokenId().getHex().equals(tokenId))
						.map(SlpUtxo::getAmount).orElse(BigDecimal.ZERO));
			}
		}

		if (totalPreviousTxTokenValue.signum() == 0 && currentTransactionTotalSlpTokenValue.signum() == 0) {
			return SlpValid.valid("prevTx and currentTx has output val 0 currentTx=" + txId);
		}

		if (currentTransactionTotalSlpTokenValue.signum() <= 0 && tokenType.equals(SlpTokenType.NFT1_CHILD.getType())) {
			return SlpValid.valid("Valid 0 output cause of valid NFT1_GENESIS tokenType=" + tokenType);
		}

		if (currentTransactionTotalSlpTokenValue.signum() == 0 && totalPreviousTxTokenValue.signum() > 0) {
			return SlpValid.invalid("Current token output val is 0 txId=" + txId + " prevTx=" + totalPreviousTxTokenValue);
		}

		if (currentTransactionTotalSlpTokenValue.signum() == 0 && totalPreviousTxTokenValue.signum() < 0) {
			return SlpValid.valid("Current token output val is 0 txId=" + txId + "tokenType=" + tokenType);
		}

		if (currentTransactionTotalSlpTokenValue.signum() == -1) {
			logger.info("CurrentTxValue is less than 0 txId={}", txId);
			return SlpValid.invalid("CurrentTxValue is less than 0 txId=" + txId);
		}

		if (currentTransactionTotalSlpTokenValue.compareTo(totalPreviousTxTokenValue) > 0) {
			logger.info("CurrentTxValue is larger than previousValue txId={} current={} prev={}", txId, currentTransactionTotalSlpTokenValue, totalPreviousTxTokenValue);
			return SlpValid.invalid("CurrentTxValue is larger than previousValue txId=" + txId +
					" current=" + currentTransactionTotalSlpTokenValue
					+ " prev=" + totalPreviousTxTokenValue
					+ " prevTxIds=" + String.join(" : ", prevTxs));
		}

		return SlpValid.valid("Tx has correct values and is valid currentSlpValue=" + currentTransactionTotalSlpTokenValue + " prevTokenValue=" + totalPreviousTxTokenValue);
	}
}