package com.bitcoin.indexer.facade.validators;

import java.math.BigDecimal;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bitcoin.indexer.blockchain.domain.IndexerTransaction;
import com.bitcoin.indexer.blockchain.domain.Input;
import com.bitcoin.indexer.blockchain.domain.Utxo;
import com.bitcoin.indexer.blockchain.domain.slp.SlpUtxo;
import com.bitcoin.indexer.blockchain.domain.slp.SlpValid;
import com.bitcoin.indexer.blockchain.domain.slp.SlpValid.Valid;
import com.bitcoin.indexer.core.Coin;
import com.bitcoin.indexer.repository.TransactionRepository;
import com.bitcoin.indexer.repository.UtxoRepository;

public class MintValidatorAssumeParentValid implements SlpValidatorFacade {

	private TransactionRepository transactionRepository;
	private UtxoRepository utxoRepository;
	private static final Logger logger = LoggerFactory.getLogger(MintValidatorAssumeParentValid.class);

	public MintValidatorAssumeParentValid(TransactionRepository transactionRepository, UtxoRepository utxoRepository) {
		this.transactionRepository = transactionRepository;
		this.utxoRepository = utxoRepository;
	}

	@Override
	public SlpValid isValid(String txId, String tokenId, String tokenType, List<Utxo> utxos, List<Input> inputs, SlpValidatorFacade baseValidator) {
		boolean validMint = false;

		BigDecimal currentTransactionTotalSlpTokenValue = utxos.stream().filter(e -> e.getSlpUtxo().isPresent())
				.map(e -> e.getSlpUtxo().get().getAmount())
				.reduce(BigDecimal::add).orElse(BigDecimal.ZERO);

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

			if (prevTx.getTransaction().getOutputs().get(input.getIndex())
					.getSlpUtxo().map(slpUtxo -> !slpUtxo.getTokenType().equals(tokenType)).orElse(false)) {
				logger.info("Minting is not matching prevTx tokentype={} txId={}", tokenType, txId);
				return SlpValid.invalid("Minting is not matching prevTx tokentype=" + tokenType + " txId=" + txId);
			}

			if (prevTx.getTransaction()
					.getSlpValid().map(prevValid -> prevValid.getValid() == Valid.INVALID).orElse(false)) {
				logger.info("Minting requires valid parent txId={} prevTx={}", txId, prevTx.getTransaction().getTxId());
				return SlpValid.invalid("Minting requires valid parent txId=" + txId + "  prevTx=" + prevTx);
			}

			validMint = prevTx.getTransaction().getOutputs().get(input.getIndex()).getSlpUtxo()
					.filter(e -> e.getSlpTokenId().getHex().equals(tokenId))
					.map(SlpUtxo::hasBaton).orElse(false);
			if (validMint) {
				return SlpValid.valid("Mint is valid prev utxo has baton for this tokenId tokenId=" + tokenId + " txId=" + txId);
			}
		}

		logger.info("Invalid mint for txId={} currentTxValue={}", txId, currentTransactionTotalSlpTokenValue);

		return SlpValid.invalid("Invalid mint for txId=" + txId + " currentTxValue=" + currentTransactionTotalSlpTokenValue);
	}
}
