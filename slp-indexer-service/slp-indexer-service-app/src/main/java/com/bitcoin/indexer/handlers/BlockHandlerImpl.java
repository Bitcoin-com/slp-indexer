package com.bitcoin.indexer.handlers;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bitcoin.indexer.blockchain.domain.Block;
import com.bitcoin.indexer.repository.BlockRepository;

import io.reactivex.Completable;

public class BlockHandlerImpl implements BlockHandler {

	private BlockRepository blockRepository;
	private static final Logger logger = LoggerFactory.getLogger(BlockHandlerImpl.class);

	public BlockHandlerImpl(BlockRepository blockRepository) {
		this.blockRepository = Objects.requireNonNull(blockRepository);
	}

	@Override
	public Completable handleBlock(Block block) {


		return blockRepository.saveBlock(block)
				.zipWith(blockRepository.saveHeight(block.getHeight()), (a, b) -> a)
				.doOnError(er -> logger.error("Could not save block hash={} height={}", block.getHash(), block.getHeight(), er))
				.ignoreElement();
	}
}
