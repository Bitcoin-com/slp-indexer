package com.bitcoin.indexer.repository.db;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.query.Update;

import com.bitcoin.indexer.blockchain.domain.Block;

@Document(collection = "blocks")
public class BlockDbObject {

	@Id
	private String hash;

	private long height;

	private String prevBlockHash;

	private String coinbaseString;

	private List<String> txIds;

	private String merkelRoot;

	private long nounce;

	private BigDecimal blockReward;

	private String chainWork;

	private BigDecimal difficulty;

	private long size;

	private long version;

	private Instant time;

	private String bits;

	public BlockDbObject(String hash,
			long height,
			String prevBlockHash,
			String coinbaseString,
			List<String> txIds,
			String merkelRoot,
			long nounce,
			BigDecimal blockReward,
			String chainWork,
			BigDecimal difficulty,
			long size,
			long version,
			Instant time,
			String bits) {
		this.hash = hash;
		this.height = height;
		this.prevBlockHash = prevBlockHash;
		this.coinbaseString = coinbaseString;
		this.txIds = txIds;
		this.merkelRoot = merkelRoot;
		this.nounce = nounce;
		this.blockReward = blockReward;
		this.chainWork = chainWork;
		this.difficulty = difficulty;
		this.size = size;
		this.version = version;
		this.time = time;
		this.bits = bits;
	}

	public Update toUpdate() {
		Update update = new Update();
		update.set("height", height);
		update.set("prevBlockHash", prevBlockHash);
		update.set("coinbaseString", coinbaseString);
		update.set("txIds", txIds);
		update.set("merkelRoot", merkelRoot);
		update.set("nounce", nounce);
		update.set("blockReward", blockReward);
		update.set("chainWork", chainWork);
		update.set("difficulty", difficulty);
		update.set("size", size);
		update.set("version", version);
		update.set("time", time);
		update.set("bits", bits);
		return update;
	}

	public static BlockDbObject fromDomain(Block block) {
		return new BlockDbObject(block.getHash(),
				block.getHeight(),
				block.getPrevBlockHash(),
				block.getCoinbaseString(),
				block.getTxIds(),
				block.getMerkelRoot(),
				block.getNounce(),
				block.getBlockReward(),
				block.getChainWork(),
				block.getDifficulty(),
				block.getSize(),
				block.getVersion(),
				block.getTime(),
				block.getBits());
	}

	public Block toDomain() {
		return new Block(Collections.emptyList(),
				hash,
				height,
				prevBlockHash,
				coinbaseString,
				txIds,
				merkelRoot,
				nounce,
				blockReward,
				chainWork,
				difficulty,
				size,
				version,
				time,
				bits);
	}
}
