package com.bitcoin.indexer.config;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.bitcoinj.core.AbstractBlockChain;
import org.bitcoinj.core.Block;
import org.bitcoinj.core.Context;
import org.bitcoinj.core.FilteredBlock;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.PrunedException;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.StoredBlock;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionOutputChanges;
import org.bitcoinj.core.VerificationException;
import org.bitcoinj.store.BlockStore;
import org.bitcoinj.store.BlockStoreException;
import org.bitcoinj.wallet.Wallet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bitcoin.indexer.blockchain.domain.Genesis;
import com.bitcoin.indexer.listener.BitcoinJListener;

public class BlockchainExtended extends AbstractBlockChain {
	private static final Logger logger = LoggerFactory.getLogger(BlockchainExtended.class);

	/**
	 * Keeps a map of block hashes to StoredBlocks.
	 */
	protected final BlockStore blockStore;
	private BitcoinJListener bitcoinJListener;
	private String lastHandledBlockHash = "";

	/**
	 * <p>Constructs a BlockChain connected to the given wallet and store. To obtain a {@link Wallet} you can construct
	 * one from scratch, or you can deserialize a saved wallet from disk using
	 * </p>
	 *
	 * <p>For the store, you should use {@link org.bitcoinj.store.SPVBlockStore} or you could also try a
	 * {@link org.bitcoinj.store.MemoryBlockStore} if you want to hold all headers in RAM and don't care about
	 * disk serialization (this is rare).</p>
	 */
	public BlockchainExtended(Context context, Wallet wallet, BlockStore blockStore) throws BlockStoreException {
		this(context, new ArrayList<Wallet>(), blockStore);
		addWallet(wallet);
	}

	/**
	 * See {@link #BlockchainExtended(Context, Wallet, BlockStore)}}
	 */
	public BlockchainExtended(NetworkParameters params, Wallet wallet, BlockStore blockStore) throws BlockStoreException {
		this(Context.getOrCreate(params), wallet, blockStore);
	}

	/**
	 * Constructs a BlockChain that has no wallet at all. This is helpful when you don't actually care about sending
	 * and receiving coins but rather, just want to explore the network data structures.
	 */
	public BlockchainExtended(Context context, BlockStore blockStore) throws BlockStoreException {
		this(context, new ArrayList<Wallet>(), blockStore);
	}

	/**
	 * See {@link #BlockchainExtended(Context, BlockStore)}
	 */
	public BlockchainExtended(NetworkParameters params, BlockStore blockStore) throws BlockStoreException {
		this(params, new ArrayList<Wallet>(), blockStore);
	}

	/**
	 * Constructs a BlockChain connected to the given list of listeners and a store.
	 */
	public BlockchainExtended(Context params, List<? extends Wallet> wallets, BlockStore blockStore) throws BlockStoreException {
		super(params, wallets, blockStore);
		this.blockStore = blockStore;
	}

	/**
	 * See {@link #BlockchainExtended(Context, List, BlockStore)}
	 */
	public BlockchainExtended(NetworkParameters params, List<? extends Wallet> wallets, BlockStore blockStore) throws BlockStoreException {
		this(Context.getOrCreate(params), wallets, blockStore);
	}

	public BlockchainExtended(Context context, BlockStore blockStore, BitcoinJListener bitcoinJListener) throws BlockStoreException {
		this(context, new ArrayList<>(), blockStore, bitcoinJListener);
	}

	public BlockchainExtended(Context context, ArrayList<Wallet> wallets, BlockStore blockStore,
			BitcoinJListener bitcoinJListener) throws BlockStoreException {
		super(context, wallets, blockStore);
		this.bitcoinJListener = bitcoinJListener;
		this.blockStore = blockStore;
	}

	@Override
	protected StoredBlock addToBlockStore(StoredBlock storedPrev, Block blockHeader, TransactionOutputChanges txOutChanges)
			throws BlockStoreException, VerificationException {
		StoredBlock newBlock = storedPrev.build(blockHeader);
		blockStore.put(newBlock);
		return newBlock;
	}

	@Override
	protected void connectBlock(Block block, StoredBlock storedPrev, boolean expensiveChecks, @Nullable List<Sha256Hash> filteredTxHashList, @Nullable Map<Sha256Hash, Transaction> filteredTxn)
			throws BlockStoreException, VerificationException, PrunedException {
		checkState(lock.isHeldByCurrentThread());
		boolean filtered = filteredTxHashList != null && filteredTxn != null;
		// Check that we aren't connecting a block that fails a checkpoint check
		if (!params.passesCheckpoint(storedPrev.getHeight() + 1, block.getHash())) {
			throw new VerificationException("Block failed checkpoint lockin at " + (storedPrev.getHeight() + 1));
		}
		if (shouldVerifyTransactions()) {
			checkNotNull(block.getTransactions());
			for (Transaction tx : block.getTransactions()) {
				if (!tx.isFinal(storedPrev.getHeight() + 1, block.getTimeSeconds())) {
					throw new VerificationException("Block contains non-final transaction");
				}
			}
		}

		StoredBlock head = getChainHead();

		if (storedPrev.equals(head)) {
			if (filtered && filteredTxn.size() > 0) {
				logger.debug("Block {} connects to top of best chain with {} transaction(s) of which we were sent {}",
						block.getHashAsString(), filteredTxHashList.size(), filteredTxn.size());
				for (Sha256Hash hash : filteredTxHashList) {
					logger.debug("  matched tx {}", hash);
				}
			}
			if (expensiveChecks && block.getTimeSeconds() <= getMedianTimestampOfRecentBlocks(head, blockStore)) {
				throw new VerificationException("Block's timestamp is too early");
			}

			// BIP 66 & 65: Enforce block version 3/4 once they are a supermajority of blocks
			// NOTE: This requires 1,000 blocks since the last checkpoint (on main
			// net, less on test) in order to be applied. It is also limited to
			// stopping addition of new v2/3 blocks to the tip of the chain.
			if (block.getVersion() == Block.BLOCK_VERSION_BIP34
					|| block.getVersion() == Block.BLOCK_VERSION_BIP66) {
				final Integer count = versionTally.getCountAtOrAbove(block.getVersion() + 1);
				if (count != null
						&& count >= params.getMajorityRejectBlockOutdated()) {
					throw new VerificationException.BlockVersionOutOfDate(block.getVersion());
				}
			}

			// This block connects to the best known block, it is a normal continuation of the system.
			TransactionOutputChanges txOutChanges = null;
			if (shouldVerifyTransactions()) {
				txOutChanges = connectTransactions(storedPrev.getHeight() + 1, block);
			}

			handleGenesis(storedPrev);

			if (!lastHandledBlockHash.equals(block.getHashAsString())) {
				try {
					bitcoinJListener.onBlocksDownloaded(block, storedPrev.getHeight() + 1);
					lastHandledBlockHash = block.getHashAsString();
				} catch (Exception e) {
					logger.error("Download block handler failed exiting before ruining the SPV blockhash={}", block.getHashAsString(), e);
					while (true) {
						try {
							Thread.sleep(1000);
							logger.error("Retrying block handler for hash={}", block.getHashAsString());
							bitcoinJListener.onBlocksDownloaded(block, storedPrev.getHeight() + 1);
							lastHandledBlockHash = block.getHashAsString();
							break;
						} catch (Exception ex) {
						}
					}
				}
			}
			StoredBlock newStoredBlock = addToBlockStore(storedPrev, block.getTransactions() == null ? block : block.cloneAsHeader(), txOutChanges);

			versionTally.add(block.getVersion());
			setChainHead(newStoredBlock);
			logger.debug("Chain is now {} blocks high, running listeners", newStoredBlock.getHeight());
			informListenersForNewBlock(block, NewBlockType.BEST_CHAIN, filteredTxHashList, filteredTxn, newStoredBlock);
		} else {
			// This block connects to somewhere other than the top of the best known chain. We treat these differently.
			//
			// Note that we send the transactions to the wallet FIRST, even if we're about to re-organize this block
			// to become the new best chain head. This simplifies handling of the re-org in the Wallet class.
			StoredBlock newBlock = storedPrev.build(block);
			boolean haveNewBestChain = newBlock.moreWorkThan(head);
			if (haveNewBestChain) {
				logger.info("Block is causing a re-organize");
			} else {
				StoredBlock splitPoint = findSplit(newBlock, head, blockStore);
				if (splitPoint != null && splitPoint.equals(newBlock)) {
					// newStoredBlock is a part of the same chain, there's no fork. This happens when we receive a block
					// that we already saw and linked into the chain previously, which isn't the chain head.
					// Re-processing it is confusing for the wallet so just skip.
					logger.warn("Saw duplicated block in main chain at height {}: {}",
							newBlock.getHeight(), newBlock.getHeader().getHash());
					return;
				}
				if (splitPoint == null) {
					// This should absolutely never happen
					// (lets not write the full block to disk to keep any bugs which allow this to happen
					//  from writing unreasonable amounts of data to disk)
					throw new VerificationException("Block forks the chain but splitPoint is null");
				} else {
					// We aren't actually spending any transactions (yet) because we are on a fork
					addToBlockStore(storedPrev, block);
					int splitPointHeight = splitPoint.getHeight();
					String splitPointHash = splitPoint.getHeader().getHashAsString();
					logger.info("Block forks the chain at height {}/block {}, but it did not cause a reorganize:\n{}",
							splitPointHeight, splitPointHash, newBlock.getHeader().getHashAsString());
				}
			}

			// We may not have any transactions if we received only a header, which can happen during fast catchup.
			// If we do, send them to the wallet but state that they are on a side chain so it knows not to try and
			// spend them until they become activated.
			if (block.getTransactions() != null || filtered) {
				informListenersForNewBlock(block, NewBlockType.SIDE_CHAIN, filteredTxHashList, filteredTxn, newBlock);
			}

			if (haveNewBestChain) {
				handleNewBestChain(storedPrev, newBlock, block, expensiveChecks);
			}
		}
	}

	private void handleGenesis(StoredBlock storedPrev) {
		try {
			if (storedPrev.getHeight() + 1 == 1) {
				logger.info("Adding genesis to index");
				bitcoinJListener.onBlocksDownloaded(Genesis.GENESIS, 0);
			}
		} catch (Exception e) {
			try {
				logger.error("Error handling genesis block handler for hash={}", storedPrev.getHeader().getHashAsString(), e);
				Thread.sleep(5000);
			} catch (InterruptedException ex) {
			}
			handleGenesis(storedPrev);
		}
	}

	@Override
	protected StoredBlock addToBlockStore(StoredBlock storedPrev, Block blockHeader)
			throws BlockStoreException, VerificationException {
		StoredBlock newBlock = storedPrev.build(blockHeader);
		blockStore.put(newBlock);
		return newBlock;
	}

	@Override
	protected void rollbackBlockStore(int height) throws BlockStoreException {
		lock.lock();
		try {
			int currentHeight = getBestChainHeight();
			checkArgument(height >= 0 && height <= currentHeight, "Bad height: %s", height);
			if (height == currentHeight) {
				return; // nothing to do
			}

			// Look for the block we want to be the new chain head
			StoredBlock newChainHead = blockStore.getChainHead();
			while (newChainHead.getHeight() > height) {
				newChainHead = newChainHead.getPrev(blockStore);
				if (newChainHead == null) {
					throw new BlockStoreException("Unreachable height");
				}
			}

			// Modify store directly
			blockStore.put(newChainHead);
			this.setChainHead(newChainHead);
		} finally {
			lock.unlock();
		}
	}

	@Override
	protected boolean shouldVerifyTransactions() {
		return false;
	}

	@Override
	protected TransactionOutputChanges connectTransactions(int height, Block block) {
		// Don't have to do anything as this is only called if(shouldVerifyTransactions())
		throw new UnsupportedOperationException();
	}

	@Override
	protected TransactionOutputChanges connectTransactions(StoredBlock newBlock) {
		// Don't have to do anything as this is only called if(shouldVerifyTransactions())
		throw new UnsupportedOperationException();
	}

	@Override
	protected void disconnectTransactions(StoredBlock block) {
		// Don't have to do anything as this is only called if(shouldVerifyTransactions())
		throw new UnsupportedOperationException();
	}

	@Override
	protected void doSetChainHead(StoredBlock chainHead) throws BlockStoreException {
		blockStore.setChainHead(chainHead);
	}

	@Override
	protected void notSettingChainHead() throws BlockStoreException {
		// We don't use DB transactions here, so we don't need to do anything
	}

	@Override
	protected StoredBlock getStoredBlockInCurrentScope(Sha256Hash hash) throws BlockStoreException {
		return blockStore.get(hash);
	}

	@Override
	public boolean add(FilteredBlock block) throws VerificationException, PrunedException {
		boolean success = super.add(block);
		if (success) {
			trackFilteredTransactions(block.getTransactionCount());
		}
		return success;
	}
}
