package com.bitcoin.indexer.facade.validators;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bitcoin.indexer.blockchain.domain.Input;
import com.bitcoin.indexer.blockchain.domain.Utxo;
import com.bitcoin.indexer.blockchain.domain.slp.SlpTokenType;
import com.bitcoin.indexer.blockchain.domain.slp.SlpUtxo;
import com.bitcoin.indexer.blockchain.domain.slp.SlpValid;
import com.bitcoin.indexer.repository.TransactionRepository;
import com.bitcoin.indexer.repository.UtxoRepository;
import com.bitcoin.indexer.core.Coin;

public class GenesisValidatorAssumeParentValid implements SlpValidatorFacade {
	private TransactionRepository transactionRepository;
	private UtxoRepository utxoRepository;
	private static final Logger logger = LoggerFactory.getLogger(GenesisValidatorAssumeParentValid.class);

	public GenesisValidatorAssumeParentValid(TransactionRepository transactionRepository, UtxoRepository utxoRepository) {
		this.transactionRepository = Objects.requireNonNull(transactionRepository);
		this.utxoRepository = Objects.requireNonNull(utxoRepository);
	}

	@Override
	public SlpValid isValid(String txId, String tokenId, String tokenType, List<Utxo> utxos, List<Input> inputs, SlpValidatorFacade baseValidator) {
		BigDecimal currentTransactionTotalSlpTokenValue = utxos.stream().filter(e -> e.getSlpUtxo().isPresent())
				.map(e -> e.getSlpUtxo().get().getAmount())
				.reduce(BigDecimal::add).orElse(BigDecimal.ZERO);

		if (tokenType.equals("UNKNOWN")) {
			logger.info("Invalid cause type unknown txId={}", txId);
			return SlpValid.invalid("Invalid cause type unknown txId=" + txId);
		}

		if (currentTransactionTotalSlpTokenValue.signum() == 0 || currentTransactionTotalSlpTokenValue.signum() == -1) {
			if (onlySlpUtxo(utxos).noneMatch(SlpUtxo::hasBaton)) {
				logger.info("Invalid cause currentTxValue is zero or less and non existing baton txId={}", txId);
				return SlpValid.invalid("Invalid cause currentTxValue is zero or less txId=" + txId);
			}
		}
		Map<Input, Utxo> inputsUtxo = new HashMap<>();
		Map<Integer, Utxo> indexUtxo = new HashMap<>();
		for (int i = 0, inputsSize = inputs.size(); i < inputsSize; i++) {
			Input input = inputs.get(i);
			Utxo utxoFromPrev = utxoRepository.fetchUtxo(input.getTxId(), input.getIndex(), Coin.BCH).blockingGet();
			if (utxoFromPrev == null) {
				continue;
			}

			inputsUtxo.put(input, utxoFromPrev);
			indexUtxo.put(i, utxoFromPrev);
		}

		if (indexUtxo.containsKey(0) && indexUtxo.containsKey(1) && tokenType.equals(SlpTokenType.NFT1_CHILD.getType())) {
			Utxo first = indexUtxo.get(0);
			Utxo second = indexUtxo.get(1);
			if (first.getSlpUtxo().isEmpty() && second.getSlpUtxo().isPresent()) {
				SlpUtxo secondSlp = second.getSlpUtxo().get();
				if (secondSlp.getTokenType().equals(SlpTokenType.NFT1_GENESIS.getType()) && secondSlp.getAmount().equals(BigDecimal.ONE)) {
					logger.info("When the first input is change output from a SLP-valid NFT1 parent SEND tx and second second input is an SLP-valid NFT1 parent SEND tx, the NFT1 child GENESIS tx w/ qty=1");
					return SlpValid.invalid("When the first input is change output from a SLP-valid NFT1 parent SEND tx and second second input is an SLP-valid NFT1 parent SEND tx, the NFT1 child GENESIS tx w/ qty=1");
				}
			}

			if (first.getSlpUtxo().isPresent() && second.getSlpUtxo().isPresent()) {
				SlpUtxo firstSlp = first.getSlpUtxo().get();
				SlpUtxo secondSlp = second.getSlpUtxo().get();
				if (firstSlp.hasBaton() && firstSlp.getTokenType().equals(SlpTokenType.NFT1_GENESIS.getType())) {
					if (secondSlp.getTokenType().equals(SlpTokenType.NFT1_GENESIS.getType()) && secondSlp.getAmount().equals(BigDecimal.ONE)) {
						logger.info("When the first input is the mint baton from a SLP-valid NFT1 parent MINT tx and second second input is an SLP-valid NFT1 parent MINT tx, the NFT1 child GENESIS tx w/ qty=1");
						return SlpValid.invalid("When the first input is the mint baton from a SLP-valid NFT1 parent MINT tx and second second input is an SLP-valid NFT1 parent MINT tx, the NFT1 child GENESIS tx w/ qty=1");
					}
				}
			}
		}

		for (Entry<Input, Utxo> inputUtxo : inputsUtxo.entrySet()) {
			Utxo utxoFromPrev = inputUtxo.getValue();
			if (utxoFromPrev.getSlpUtxo().map(SlpUtxo::isGenesis).orElse(false)) {
				if (utxoFromPrev.getSlpUtxo()
						.map(prevSlpUtxo -> prevSlpUtxo.getTokenType().equals(SlpTokenType.NFT1_GENESIS.getType()) && tokenType.equals(SlpTokenType.NFT1_CHILD.getType())).orElse(false)) {
					if (utxoFromPrev.getSlpUtxo()
							.map(SlpUtxo::getAmount).orElse(BigDecimal.ZERO).signum() == 0) {
						logger.info("PrevTx is genesis but with zero output txId={}", txId);
						return SlpValid.invalid("PrevTx is genesis but with zero output txId=" + txId);
					}
				}

				if (utxoFromPrev.getSlpUtxo().get().getTokenType().equals(SlpTokenType.PERMISSIONLESS.getType())) {
					if (onlySlpUtxo(utxos).anyMatch(e -> e.getTokenType().equals(SlpTokenType.NFT1_CHILD.getType()))) {
						return SlpValid.invalid("Invalid cause parent is a type 1 genesis and utxo is NFT1_CHILD");
					}
				}

				if (onlySlpUtxo(utxos)
						.anyMatch(SlpUtxo::isGenesis)) {

					if (utxoFromPrev.getSlpUtxo().get().getTokenType().equals(SlpTokenType.NFT1_GENESIS.getType()) && inputs.size() > 1) {
						if (tokenType.equals(SlpTokenType.NFT1_CHILD.getType())) {
							if (onlySlpUtxo(utxos).anyMatch(slpUtxo -> slpUtxo.getAmount().compareTo(BigDecimal.ONE) == 0)) {
								return SlpValid.invalid("NFT1 parent GENESIS tx, the NFT1 child GENESIS tx w/ qty=1 should be SLP-invalid");
							}
						}
					}

					if (onlySlpUtxo(utxos)
							.map(SlpUtxo::getAmount).reduce(BigDecimal::add).orElse(BigDecimal.ZERO).equals(BigDecimal.ONE)) {
						return SlpValid.valid("prev is genesis and current has amount == 1");
					}

					if (onlySlpUtxo(utxos)
							.filter(e -> e.getTokenType().equals(SlpTokenType.NFT1_GENESIS.getType()) || e.getTokenType().equals(SlpTokenType.NFT1_CHILD.getType()))
							.map(SlpUtxo::getAmount).reduce(BigDecimal::add).orElse(BigDecimal.ZERO).signum() > 0) {
						logger.info("Prev Tx is genesis currentValue has to be more than > 0 txId={}", txId);
						return SlpValid.invalid("Prev Tx is genesis currentValue has to be more than > 0 txId=" + txId);
					}

					if (onlySlpUtxo(utxos).count() >= 1) {
						return SlpValid.valid("Has one slp utxo");
					}

					logger.info("Invalid since non of the above is true txId={}", txId);
					return SlpValid.invalid("Invalid since non of the above is true txId=" + txId);
				}
			}
		}

		return SlpValid.valid("Genesis is a normal one and valid");
	}

	@NotNull
	private Stream<SlpUtxo> onlySlpUtxo(List<Utxo> utxos) {
		return utxos.stream().filter(e -> e.getSlpUtxo().isPresent()).map(e -> e.getSlpUtxo().get());
	}
}
