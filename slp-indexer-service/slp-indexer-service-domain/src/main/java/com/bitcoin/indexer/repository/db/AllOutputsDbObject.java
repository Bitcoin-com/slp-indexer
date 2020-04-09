package com.bitcoin.indexer.repository.db;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import org.spongycastle.util.encoders.Hex;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.query.Update;

import com.bitcoin.indexer.blockchain.domain.Address;
import com.bitcoin.indexer.blockchain.domain.Utxo;
import com.bitcoin.indexer.blockchain.domain.slp.Script;
import com.bitcoin.indexer.blockchain.domain.slp.SlpTokenId;
import com.bitcoin.indexer.blockchain.domain.slp.SlpUtxo;
import com.bitcoin.indexer.blockchain.domain.slp.SlpValid;
import com.bitcoin.indexer.core.Coin;

@Document(collection = "allOutputs")
public class AllOutputsDbObject implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	private String id;

	private String txId;

	private int index;

	private String coin;

	private String scriptPubKey;

	private String address;

	private String value;

	private Instant timestamp;

	private Instant spendingTimestamp;

	private boolean confirmed;

	private boolean isSpent;

	private SlpUtxoType slpUtxoType;

	private Integer confirmedHeight;

	public AllOutputsDbObject(String txId,
			int index,
			String scriptPubKey,
			String address,
			BigDecimal value,
			Instant timestamp,
			Instant spendingTimestamp,
			boolean confirmed,
			boolean isSpent,
			String coin,
			SlpUtxoType slpUtxoType,
			Integer confirmedHeight) {
		this.id = keyParser(txId, index);
		this.txId = txId;
		this.index = index;
		this.scriptPubKey = scriptPubKey;
		this.address = address;
		this.value = value.toString();
		this.timestamp = timestamp;
		this.spendingTimestamp = spendingTimestamp;
		this.confirmed = confirmed;
		this.isSpent = isSpent;
		this.coin = coin;
		this.slpUtxoType = slpUtxoType;
		this.confirmedHeight = confirmedHeight;
	}

	public AllOutputsDbObject() {
	}

	public static AllOutputsDbObject fromDomain(Utxo utxo,
			Instant spendingTimestamp,
			boolean isSpent,
			Coin coin) {
		Optional<SlpUtxo> slpUtxo = utxo.getSlpUtxo();
		return new AllOutputsDbObject(
				utxo.getTxId(),
				utxo.getIndex(),
				utxo.getScriptPubkey(),
				utxo.getAddress().getAddress(),
				utxo.getAmount(),
				utxo.getTimestamp(),
				spendingTimestamp,
				utxo.isConfirmed(),
				isSpent,
				coin.name(),
				slpUtxo.map(u -> new SlpUtxoType(u.getSlpTokenId().toString(),
						u.getTokenTransactionType(),
						u.getTokenType(),
						u.getAmount().toString(),
						u.hasBaton(),
						u.getTokenTicker(),
						u.getSlpTokenName(), SlpValidDbType.fromDomain(u.getParentTransactionValid()),
						u.getTokenTypeHex()))
						.orElse(null),
				utxo.getConfirmedHeight().orElse(null));
	}

	public Utxo toDomain() {
		return Utxo.create(txId,
				Address.create(address),
				scriptPubKey,
				new BigDecimal(value),
				confirmed,
				index,
				isSpent,
				timestamp,
				Optional.ofNullable(slpUtxoType).map(e -> SlpUtxo.create(new SlpTokenId(e.getSlpTokenId()),
						new BigDecimal(e.getAmount()),
						e.isHasBaton(),
						e.getTokenTicker(),
						e.getTokenTransactionType(),
						e.getTokenName(),
						e.getTokenType(),
						e.getTokenTypeHex(),
						SlpValid.create(e.getParentTransactionValid().getReason(), e.getParentTransactionValid().getValid()))).orElse(null),
				new Script(Hex.decode(scriptPubKey)).isOpReturn(),
				Optional.ofNullable(confirmedHeight).orElse(null));
	}

	public Update toUpdate() {
		Update update = new Update();
		update.set("txId", txId);
		update.set("index", index);
		update.set("coin", coin);
		update.set("address", address);
		update.set("scriptPubKey", scriptPubKey);
		update.set("value", value);
		update.set("timestamp", timestamp);
		update.set("spendingTimestamp", spendingTimestamp);
		update.set("confirmed", confirmed);
		update.set("isSpent", isSpent);
		update.set("confirmedHeight", confirmedHeight);
		if (slpUtxoType != null) {
			update.set("slpUtxoType", slpUtxoType.toDocument());
		}
		return update;
	}

	public Update partialUpdate() {
		Update update = new Update();
		if (slpUtxoType != null) {
			update.set("slpUtxoType.parentTransactionValid", slpUtxoType.getParentTransactionValid().toDocument());
		}
		return update;
	}

	public String getId() {
		return id;
	}

	public String getTxId() {
		return txId;
	}

	public int getIndex() {
		return index;
	}

	public String getCoin() {
		return coin;
	}

	public String getScriptPubKey() {
		return scriptPubKey;
	}

	public String getAddress() {
		return address;
	}

	public BigDecimal getValue() {
		return new BigDecimal(value);
	}

	public Instant getTimestamp() {
		return timestamp;
	}

	public Instant getSpendingTimestamp() {
		return spendingTimestamp;
	}

	public boolean isConfirmed() {
		return confirmed;
	}

	public boolean isSpent() {
		return isSpent;
	}

	public SlpUtxoType getSlpUtxoType() {
		return slpUtxoType;
	}

	public Integer getConfirmedHeight() {
		return confirmedHeight;
	}

	public static String keyParser(String txId, int index) {
		return txId + ":" + index;
	}
}
