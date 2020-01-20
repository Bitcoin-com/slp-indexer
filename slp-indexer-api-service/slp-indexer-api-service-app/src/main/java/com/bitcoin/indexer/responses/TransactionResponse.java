package com.bitcoin.indexer.responses;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import com.bitcoin.indexer.blockchain.domain.Transaction;
import com.bitcoin.indexer.blockchain.domain.slp.SlpOpReturn;
import com.bitcoin.indexer.blockchain.domain.slp.SlpValid;

public class TransactionResponse {

	public List<UtxoResponse> outputs;
	public List<InputResponse> inputs;
	public String txId;
	public boolean confirmed;
	public BigDecimal fees;
	public Instant time;
	public boolean fromBlock;
	public String blockHash;
	public Integer blockHeight;
	public List<SlpOpReturn> slpOpReturn;
	public SlpValid slpValid;
	public String rawHex;
	public long version;
	public long locktime;
	public long size;
	public Instant blockTime;

	public TransactionResponse() {
	}

	public static TransactionResponse fromDomain(Transaction transaction) {
		return new TransactionResponse(
				transaction.getOutputs().stream().map(e -> new UtxoResponse()).collect(Collectors.toList()),
				transaction.getInputs().stream().map(e -> new InputResponse("test")).collect(Collectors.toList()),
				transaction.getTxId(),
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
				transaction.getBlockTime().orElse(null)
		);
	}

	public TransactionResponse(List<UtxoResponse> outputs,
			List<InputResponse> inputs,
			String txId,
			boolean confirmed,
			BigDecimal fees,
			Instant time,
			boolean fromBlock,
			String blockHash,
			Integer blockHeight,
			List<SlpOpReturn> slpOpReturn, SlpValid slpValid, String rawHex, long version, long locktime, long size, Instant blockTime) {
		this.outputs = outputs;
		this.inputs = inputs;
		this.txId = txId;
		this.confirmed = confirmed;
		this.fees = fees;
		this.time = time;
		this.fromBlock = fromBlock;
		this.blockHash = blockHash;
		this.blockHeight = blockHeight;
		this.slpOpReturn = slpOpReturn;
		this.slpValid = slpValid;
		this.rawHex = rawHex;
		this.version = version;
		this.locktime = locktime;
		this.size = size;
		this.blockTime = blockTime;
	}
}
