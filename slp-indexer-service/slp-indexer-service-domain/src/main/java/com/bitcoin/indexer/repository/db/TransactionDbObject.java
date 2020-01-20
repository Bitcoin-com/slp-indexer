package com.bitcoin.indexer.repository.db;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.query.Update;

import com.bitcoin.indexer.blockchain.domain.Address;
import com.bitcoin.indexer.blockchain.domain.Input;
import com.bitcoin.indexer.blockchain.domain.Transaction;
import com.bitcoin.indexer.blockchain.domain.Utxo;
import com.bitcoin.indexer.blockchain.domain.slp.SlpOpReturn;
import com.bitcoin.indexer.blockchain.domain.slp.SlpTokenId;
import com.bitcoin.indexer.blockchain.domain.slp.SlpUtxo;
import com.bitcoin.indexer.blockchain.domain.slp.SlpValid;
import com.bitcoin.indexer.core.Coin;

@Document(collection = "transactions")
public class TransactionDbObject implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	private String txId;

	private String coin;

	private List<UtxoDbType> outputs;

	private List<InputDbType> inputs;

	private BigDecimal fees;

	private Instant time;

	private String blockHash;

	private Integer blockHeight;

	private SlpValidDbType slpValid;

	private String rawHex;

	private long version;

	private long locktime;

	private long size;

	private Instant blockTime;

	public TransactionDbObject(String txId,
			String coin,
			List<UtxoDbType> outputs,
			List<InputDbType> inputs,
			BigDecimal fees,
			Instant time,
			String blockHash,
			Integer blockHeight,
			SlpValidDbType slpValid,
			String rawHex,
			long version,
			long locktime,
			long size,
			Instant blockTime) {
		this.txId = txId;
		this.coin = coin;
		this.outputs = outputs;
		this.inputs = inputs;
		this.fees = fees;
		this.time = time;
		this.blockHash = blockHash;
		this.blockHeight = blockHeight;
		this.slpValid = slpValid;
		this.rawHex = rawHex;
		this.version = version;
		this.locktime = locktime;
		this.size = size;
		this.blockTime = blockTime;
	}

	public TransactionDbObject() {
	}

	public static TransactionDbObject fromDomain(Transaction transaction, Coin coin) {
		return new TransactionDbObject(
				transaction.getTxId(),
				coin.name(),
				transaction.getOutputs().stream().map(UtxoDbType::fromDomain).collect(Collectors.toList()),
				transaction.getInputs().stream().map(InputDbType::fromDomain).collect(Collectors.toList()),
				transaction.getFees(),
				transaction.getTime(),
				transaction.getBlockHash().orElse(null),
				transaction.getBlockHeight().orElse(null),
				transaction.getSlpValid().map(v -> new SlpValidDbType(v.getReason(), v.getValid())).orElse(null),
				transaction.getRawHex(),
				transaction.getVersion(),
				transaction.getLocktime(),
				transaction.getSize(),
				transaction.getBlockTime().orElse(null));
	}

	public Update toUpdate() {
		Update update = new Update();
		update.set("coin", coin);
		update.set("inputs", inputs.stream().map(InputDbType::toDocument).collect(Collectors.toList()));
		update.set("outputs", outputs.stream().map(UtxoDbType::toDocument).collect(Collectors.toList()));
		update.set("fees", fees);
		update.set("time", time);
		update.set("blockHash", blockHash);
		update.set("blockHeight", blockHeight);
		update.set("rawHex", rawHex);
		update.set("version", version);
		update.set("locktime", locktime);
		update.set("size", size);
		update.set("blockTime", blockTime);

		if (slpValid != null) {
			update.set("slpValid", slpValid.toDocument());
		}

		return update;
	}

	public Transaction toDomain() {
		List<Utxo> utxos = outputs.stream().map(e -> Utxo.create(
				e.getTxId(),
				Address.create(e.getAddress()),
				e.getScriptPubkey(),
				new BigDecimal(e.getAmount()),
				e.isConfirmations(),
				e.getIndex(),
				false,
				time,
				e.getSlpUtxoType().map(slp -> SlpUtxo.create(
						new SlpTokenId(slp.getSlpTokenId()),
						new BigDecimal(slp.getAmount()),
						slp.isHasBaton(),
						slp.getTokenTicker(),
						slp.getTokenTransactionType(),
						slp.getTokenName(),
						slp.getTokenType()
				)).orElse(null),
				e.isOpReturn()
		)).collect(Collectors.toList());
		return Transaction.create(
				txId,
				utxos,
				inputs.stream().map(inp -> Input.knownValue(Address.create(inp.getAddress()),
						new BigDecimal(inp.getAmount()),
						inp.getInputIndex(),
						inp.getTxId(),
						inp.getSlpUtxoType().map(SlpUtxoType::toDomain).orElse(null),
						inp.isCoinbase(), inp.getSequence())).collect(Collectors.toList()),
				blockHeight != null,
				fees,
				time,
				blockHeight != null,
				blockHash,
				blockHeight,
				parse(txId, utxos).map(List::of).orElse(List.of()),
				SlpValid.create(slpValid.getReason(), slpValid.getValid()),
				rawHex,
				version,
				locktime,
				size,
				blockTime
		);
	}

	private Optional<SlpOpReturn> parse(String txId, List<Utxo> utxos) {
		if (utxos.isEmpty()) {
			return Optional.empty();
		}

		Utxo utxo = utxos.get(0);
		if (utxo.getSlpUtxo().isPresent()) {
			return Optional.ofNullable(SlpOpReturn.Companion.tryParse(txId, utxo.getScriptPubkey()));
		}

		return Optional.empty();

	}

	public String getTxId() {
		return txId;
	}

	public String getCoin() {
		return coin;
	}

	public List<UtxoDbType> getOutputs() {
		return outputs;
	}

	public List<InputDbType> getInputs() {
		return inputs;
	}

	public BigDecimal getFees() {
		return fees;
	}

	public Instant getTime() {
		return time;
	}

	public String getBlockHash() {
		return blockHash;
	}

	public Integer getBlockHeight() {
		return blockHeight;
	}

	public SlpValidDbType getSlpValid() {
		return slpValid;
	}

	public String getRawHex() {
		return rawHex;
	}
}
