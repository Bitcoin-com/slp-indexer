package com.bitcoin.indexer.listener;

import org.bitcoinj.core.Block;

public interface SPVOnPut {

	void onBlocksDownloaded(Block block, int height);

}
