package com.bitcoin.indexer.facade;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.bitcoinj.core.Coin;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionInput;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptBuilder;
import org.bitcoinj.script.ScriptChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import com.bitcoin.indexer.blockchain.domain.Address;
import com.bitcoin.indexer.blockchain.domain.BlockReward;
import com.bitcoin.indexer.blockchain.domain.Input;
import com.bitcoin.indexer.blockchain.domain.Utxo;
import com.bitcoin.indexer.blockchain.domain.slp.SlpOpReturn;
import com.bitcoin.indexer.blockchain.domain.slp.SlpOpReturnGenesis;
import com.bitcoin.indexer.blockchain.domain.slp.SlpTokenDetails;
import com.bitcoin.indexer.blockchain.domain.slp.SlpUtxoParser;
import com.bitcoin.indexer.blockchain.domain.slp.SlpValid;
import com.bitcoin.indexer.calculators.FeeCalculator;
import com.bitcoin.indexer.facade.validators.SlpValidatorFacade;
import com.bitcoin.indexer.repository.BlockRepository;
import com.bitcoin.indexer.repository.SlpDetailsRepository;
import com.bitcoin.indexer.repository.UtxoRepository;
import com.bitcoin.indexer.sorter.GenesisFirstSorter;

public class BitcoinJConverters {

	private SlpValidatorFacade slpValidatorFacade;
	private SlpDetailsRepository slpDetailsRepository;
	private UtxoRepository utxoRepository;
	private com.bitcoin.indexer.core.Coin coin;
	private BlockRepository blockRepository;
	private BigInteger pastBlockWork;

	private static final Logger logger = LoggerFactory.getLogger(BitcoinJConverters.class);

	public BitcoinJConverters(SlpDetailsRepository slpDetailsRepository,
			UtxoRepository utxoRepository,
			com.bitcoin.indexer.core.Coin coin,
			BlockRepository blockRepository) {
		this.slpDetailsRepository = Objects.requireNonNull(slpDetailsRepository);
		this.utxoRepository = Objects.requireNonNull(utxoRepository);
		this.coin = Objects.requireNonNull(coin);
		this.blockRepository = Objects.requireNonNull(blockRepository);
	}

	public com.bitcoin.indexer.blockchain.domain.Transaction transaction(
			org.bitcoinj.core.Transaction transaction,
			NetworkParameters networkParameters,
			int height,
			boolean fromBlock,
			String blockHash,
			Instant timestamp,
			Instant blockTime) {
		List<Input> inputs = getInputs(transaction, networkParameters, height);

		List<Utxo> utxos = getUtxos(transaction, networkParameters, fromBlock, timestamp, height);

		List<SlpOpReturn> slpOpReturns = getOpReturn(transaction);

		SlpValid valid = SlpValid.unknown(); //We don't know if it's valid at this stage cause of CTOR ordering
		if (!slpOpReturns.isEmpty()) {
			SlpOpReturn slpOpReturn = slpOpReturns.get(0);
			try {
				utxos = getSlpUtxos(transaction, utxos, slpOpReturn, height, fromBlock);
			} catch (Exception e) {
				logger.error("Could not parse slp txId={}", transaction.getHashAsString());
				throw new RuntimeException("Transaction with error txId" + transaction.getHashAsString(), e);
			}
		}

		BigDecimal fee = FeeCalculator.calculateFee(inputs, utxos);

		byte[] data = transaction.bitcoinSerialize();
		if (fromBlock) {
			return com.bitcoin.indexer.blockchain.domain.Transaction.fromBlock(transaction.getHashAsString(),
					utxos,
					inputs,
					true,
					fee,
					timestamp,
					true,
					blockHash,
					height,
					slpOpReturns,
					valid,
					Hex.toHexString(data),
					transaction.getVersion(),
					transaction.getLockTime(),
					data.length,
					blockTime
			);
		}

		return com.bitcoin.indexer.blockchain.domain.Transaction.fromMempool(transaction.getHashAsString(),
				utxos,
				inputs,
				fee,
				timestamp,
				slpOpReturns,
				valid,
				Hex.toHexString(data),
				transaction.getVersion(),
				transaction.getLockTime(),
				data.length);
	}

	public List<Utxo> getSlpUtxos(Transaction transaction, List<Utxo> utxos, SlpOpReturn slpOpReturn, Integer height, boolean fromBlock) {
		SlpTokenDetails slpTokenDetails = getSlpTokenDetails(slpOpReturn, height).orElseGet(() -> {
			logger.info("We should have all slptokenDetails for a valid tx id={}", transaction.getHashAsString());
			return new SlpTokenDetails(slpOpReturn.getTokenId(), "", "", 0, "", null);
		});
		if (fromBlock) {
			return SlpUtxoParser.slpParsedUtxo(slpOpReturn, utxos, slpTokenDetails, height);
		}
		return SlpUtxoParser.slpParsedUtxo(slpOpReturn, utxos, slpTokenDetails, null);
	}

	public static List<SlpOpReturn> getOpReturn(Transaction transaction) {
		return transaction.getOutputs()
				.stream()
				.filter(BitcoinJConverters::isOpReturn)
				.map(opReturnOutput -> {
					try {
						return SlpOpReturn.Companion.tryParse(transaction.getHashAsString(), Hex.toHexString(opReturnOutput.getScriptBytes()));
					} catch (Exception e) {
						return null;
					}
				})
				.filter(Objects::nonNull)
				.collect(Collectors.toList());
	}

	public List<Input> getInputs(Transaction transaction, NetworkParameters networkParameters, int height) {
		return transaction.getInputs().stream()
				.map(in -> input(in, in.getOutpoint().getHash().toString(),
						Long.valueOf(in.getOutpoint().getIndex()).intValue(),
						height,
						networkParameters))
				.collect(Collectors.toList());
	}

	public static List<Utxo> getUtxos(Transaction transaction, NetworkParameters networkParameters, boolean fromBlock, Instant timestamp, Integer height) {
		return transaction.getOutputs()
				.stream()
				.map(output -> {
					Address address = getAddress(networkParameters, output, transaction.getHashAsString());
					return getUtxo(transaction, fromBlock, output, address, timestamp, height);
				})
				.collect(Collectors.toList());
	}

	private Optional<SlpTokenDetails> getSlpTokenDetails(SlpOpReturn slpOpReturn, Integer currentBlock) {
		if (SlpUtxoParser.isGenesis(slpOpReturn)) {
			SlpOpReturnGenesis genesis = (SlpOpReturnGenesis) slpOpReturn;
			return Optional.ofNullable(slpDetailsRepository.saveSlpTokenDetails(genesis.getToDetails(), null, null, currentBlock).blockingGet());
		} else if (SlpUtxoParser.isMint(slpOpReturn)) {
			return Optional.ofNullable(slpDetailsRepository.updateMetadata(slpOpReturn.getTokenId(), currentBlock, null).blockingGet());
		} else {
			return Optional.ofNullable(slpDetailsRepository.updateMetadata(slpOpReturn.getTokenId(), null, currentBlock).blockingGet());
		}
	}

	private static Utxo getUtxo(Transaction transaction,
			boolean fromBlock,
			TransactionOutput output,
			Address addr,
			Instant timestamp,
			Integer height) {
		if (fromBlock) {
			return Utxo.confirmed(transaction.getHashAsString(),
					addr, getScript(output, transaction),
					fromCoin(output.getValue()),
					timestamp,
					output.getIndex(),
					isOpReturn(output),
					height
			);
		}
		return Utxo.unconfirmed(transaction.getHashAsString(),
				addr,
				getScript(output, transaction),
				fromCoin(output.getValue()),
				timestamp,
				output.getIndex(),
				isOpReturn(output));
	}

	private static boolean isOpReturn(TransactionOutput output) {
		try {
			return output.getScriptPubKey().isOpReturn();
		} catch (Exception e) {
			return false;
		}
	}

	private static String getScript(TransactionOutput output, Transaction transaction) {
		try {
			return Hex.toHexString(output.getScriptPubKey().getProgram());
		} catch (Exception e) {
			logger.info("Could not parse script output is corrupt txId={} index={}", transaction.getHashAsString(), output.getIndex());
			return Hex.toHexString(output.getScriptBytes());
		}
	}

	private BigDecimal getDifficulty(long target) {
		long shift = (target >> 24) & 0xff;
		double difficultyRaw = Double.longBitsToDouble(0x0000ffff) / Double.longBitsToDouble(target & 0x00ffffff);
		while (shift < 29) {
			difficultyRaw *= 256.0;
			shift += 1;
		}
		while (shift > 29) {
			difficultyRaw /= 256.0;
			shift -= 1;
		}
		return BigDecimal.valueOf(difficultyRaw);
	}

	public com.bitcoin.indexer.blockchain.domain.Block block(org.bitcoinj.core.Block block, int bestHeight, NetworkParameters networkParameters) {
		String prevBlockHash = Optional.ofNullable(block.getPrevBlockHash()).map(Sha256Hash::toString).orElse("");
		int size = block.bitcoinSerialize().length;
		BigDecimal difficulty = getDifficulty(block.getDifficultyTarget());
		String chainWork = "";
		if (block.getTransactions() != null) {
			GenesisFirstSorter genesisFirstSorter = new GenesisFirstSorter(block.getTransactions());
			List<Transaction> sortedBlock = genesisFirstSorter.getSortedBlock();
			List<com.bitcoin.indexer.blockchain.domain.Transaction> blockTxes = sortedBlock
					.stream()
					.map(transaction -> transaction(transaction, networkParameters, bestHeight, true,
							block.getHashAsString(), block.getTime().toInstant(), block.getTime().toInstant()))
					.collect(Collectors.toList());

			return new com.bitcoin.indexer.blockchain.domain.Block(blockTxes,
					block.getHashAsString(),
					bestHeight,
					prevBlockHash,
					"",
					blockTxes.stream().map(com.bitcoin.indexer.blockchain.domain.Transaction::getTxId).collect(Collectors.toList()),
					Hex.toHexString(block.getMerkleRoot().getBytes()),
					block.getNonce(),
					BlockReward.getReward(bestHeight),
					chainWork,
					difficulty,
					size,
					block.getVersion(),
					block.getTime().toInstant(),
					String.format("%08x", block.getDifficultyTarget()));
		}
		return new com.bitcoin.indexer.blockchain.domain.Block(Collections.emptyList(),
				block.getHashAsString(),
				bestHeight,
				prevBlockHash,
				"",
				Collections.emptyList(),
				Hex.toHexString(block.getMerkleRoot().getBytes()),
				block.getNonce(),
				BlockReward.getReward(bestHeight),
				chainWork,
				difficulty,
				size,
				block.getVersion(),
				block.getTime().toInstant(),
				String.format("%08x", block.getDifficultyTarget()));
	}

	private Input input(TransactionInput input,
			String hashAsString,
			int index,
			int currentHeight,
			NetworkParameters networkParameters) {
		if (input.isCoinBase()) {
			return Input.knownValue(com.bitcoin.indexer.blockchain.domain.Address.coinbase(hashAsString),
					BlockReward.getReward(currentHeight),
					index,
					hashAsString, null, true, input.getSequenceNumber());
		}
		Address fromInput = getFromInput(input, networkParameters);
		return Input.unknownValue(fromInput, index, hashAsString, false, input.getSequenceNumber());
	}

	public static com.bitcoin.indexer.blockchain.domain.Address getAddress(NetworkParameters params, TransactionOutput output, String txId) {
		try {
			Script script = output.getScriptPubKey();
			if (script.isSentToAddress()) {
				return com.bitcoin.indexer.blockchain.domain.Address.create(new org.bitcoinj.core.Address(params,
						script.getPubKeyHash()).toBase58());
			} else if (script.isPayToScriptHash()) {
				return com.bitcoin.indexer.blockchain.domain.Address.create(org.bitcoinj.core.Address.fromP2SHScript(params,
						output.getScriptPubKey()).toBase58());
			} else if (script.isSentToRawPubKey()) {
				return com.bitcoin.indexer.blockchain.domain.Address.create(ECKey.fromPublicOnly(script.getPubKey()).toAddress(params).toBase58());
			} else if (script.isOpReturn()) {
				return Address.opReturn(txId);
			}
			return Address.unknownFormat();
		} catch (Exception e) {
			return Address.unknownFormat();
		}
	}

	private static BigDecimal fromCoin(Coin coin) {
		return new BigDecimal(coin.value);
	}

	private com.bitcoin.indexer.blockchain.domain.Address getFromInput(TransactionInput input, NetworkParameters networkParameters) {
		try {
			List<ScriptChunk> scriptChunks = input.getScriptSig().getChunks();
			if (scriptChunks.isEmpty()) {
				return Address.unknownFormat();
			}
			if (scriptChunks.get(0).opcode == 0) {
				return Optional.ofNullable(scriptChunks.get(scriptChunks.size() - 1).data)
						.map(Script::new)
						.map(ScriptBuilder::createP2SHOutputScript)
						.map(outputScript -> com.bitcoin.indexer.blockchain.domain.Address.create(org.bitcoinj.core.Address.fromP2SHScript(networkParameters, outputScript).toBase58()))
						.orElse(Address.unknownFormat());
			} else {
				return com.bitcoin.indexer.blockchain.domain.Address.create(ECKey.fromPublicOnly(input.getScriptSig().getPubKey())
						.toAddress(networkParameters).toBase58());
			}
		} catch (Exception e) {
			String txId = input.getOutpoint().getHash().toString();
			int index = Long.valueOf(input.getOutpoint().getIndex()).intValue();
			Utxo prevOut = utxoRepository.fetchUtxo(txId, index, coin).blockingGet();
			if (prevOut == null) {
				return Address.unknownFormat();
			}
			return Optional.of(prevOut).map(Utxo::getAddress).orElse(Address.unknownFormat());
		}
	}
}
