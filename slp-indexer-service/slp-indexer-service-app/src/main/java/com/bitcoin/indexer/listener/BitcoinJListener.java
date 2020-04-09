package com.bitcoin.indexer.listener;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.bitcoinj.core.Block;
import org.bitcoinj.core.FilteredBlock;
import org.bitcoinj.core.GetDataMessage;
import org.bitcoinj.core.Message;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Peer;
import org.bitcoinj.core.StoredBlock;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.VerificationException;
import org.bitcoinj.core.listeners.BlocksDownloadedEventListener;
import org.bitcoinj.core.listeners.NewBestBlockListener;
import org.bitcoinj.core.listeners.OnTransactionBroadcastListener;
import org.bitcoinj.core.listeners.PeerDataEventListener;
import org.bitcoinj.core.listeners.ReorganizeListener;
import org.bitcoinj.store.BlockStore;
import org.bitcoinj.store.BlockStoreException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.util.Pair;

import com.bitcoin.indexer.blockchain.domain.IndexerTransaction;
import com.bitcoin.indexer.blockchain.domain.Input;
import com.bitcoin.indexer.blockchain.domain.Utxo;
import com.bitcoin.indexer.config.SystemTimer;
import com.bitcoin.indexer.core.Coin;
import com.bitcoin.indexer.facade.BitcoinJConverters;
import com.bitcoin.indexer.handlers.BlockHandler;
import com.bitcoin.indexer.handlers.TransactionHandler;
import com.bitcoin.indexer.repository.UtxoRepository;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.subjects.PublishSubject;

public class BitcoinJListener implements PeerDataEventListener,
		OnTransactionBroadcastListener,
		BlocksDownloadedEventListener,
		NewBestBlockListener,
		ReorganizeListener,
		SPVOnPut {

	private final Cache<String, Boolean> transactionCache = Caffeine.newBuilder()
			.expireAfterWrite(30, TimeUnit.MINUTES)
			.executor(Executors.newSingleThreadExecutor())
			.maximumSize(100000)
			.build();

	private final Cache<String, Boolean> transactionStreamCache = Caffeine.newBuilder()
			.executor(Executors.newSingleThreadExecutor())
			.maximumSize(100000)
			.build();

	private static final Logger logger = LoggerFactory.getLogger(BitcoinJListener.class);
	private final TransactionHandler transactionHandler;
	private final NetworkParameters networkParameters;
	private BlockHandler blockHandler;
	private final BlockStore blockStore;
	private final Coin coin;
	private BitcoinJConverters converters;
	private Boolean isFullMode;
	private UtxoRepository utxoRepository;

	private final Timer transactionHandlerTimer = Metrics.timer("transaction_handler_timer_listener");

	private final Flowable<com.bitcoin.indexer.blockchain.domain.Transaction> transactionFlowable;
	private final PublishSubject<com.bitcoin.indexer.blockchain.domain.Transaction> transactionPublishSubject = PublishSubject.create();

	private final Queue<Pair<Integer, Transaction>> slpValidationRetryQueue = new ConcurrentLinkedQueue<>();

	public BitcoinJListener(TransactionHandler transactionHandler,
			NetworkParameters networkParameters,
			BlockHandler blockHandler,
			BlockStore blockStore,
			Coin coin,
			BitcoinJConverters converters,
			boolean isFullMode,
			UtxoRepository utxoRepository) {
		this.transactionHandler = Objects.requireNonNull(transactionHandler);
		this.networkParameters = Objects.requireNonNull(networkParameters);
		this.blockHandler = Objects.requireNonNull(blockHandler);
		this.blockStore = Objects.requireNonNull(blockStore);
		this.coin = Objects.requireNonNull(coin);
		this.converters = Objects.requireNonNull(converters);
		this.isFullMode = isFullMode;
		this.utxoRepository = utxoRepository;
		this.transactionFlowable = transactionPublishSubject.toFlowable(BackpressureStrategy.BUFFER).publish().autoConnect();
	}

	@Override
	public void onBlocksDownloaded(Peer peer, Block block, @Nullable FilteredBlock filteredBlock, int blocksLeft) {

	}

	@Override
	public void onChainDownloadStarted(Peer peer, int blocksLeft) {
		logger.info("onChainDownloadStarted peer={} blocksLeft={}", peer, blocksLeft);
	}

	@Nullable
	@Override
	public List<Message> getData(Peer peer, GetDataMessage m) {
		return Collections.emptyList();
	}

	@Override
	public Message onPreMessageReceived(Peer peer, Message m) {
		return m;
	}

	@Override
	public void onTransaction(Peer peer, Transaction t) {
		onTransaction(t, Long.valueOf(peer.getBestHeight()).intValue());
	}

	public void onTransaction(Transaction t, int height) {
		if (transactionCache.getIfPresent(t.getHashAsString()) != null) {
			return;
		}

		logger.debug("Tx emitted saving to cache txid={}", t.getHashAsString());

		Instant firstSeen = Instant.now();
		transactionCache.put(t.getHashAsString(), true);

		com.bitcoin.indexer.blockchain.domain.Transaction transaction = converters.transaction(t,
				networkParameters,
				height,
				false,
				"",
				firstSeen,
				null
		);

		transactionPublishSubject.onNext(transaction);
		consumeTransactions(List.of(transaction));
		transactionStreamCache.put(t.getHashAsString(), true);
	}

	public void consumeTransactions(List<com.bitcoin.indexer.blockchain.domain.Transaction> transactions) {
		List<IndexerTransaction> indexerTransactions = createIndexerTransaction(transactions, false);

		logger.trace("Indexer txs={} ", indexerTransactions.size());

		if (!indexerTransactions.isEmpty()) {
			List<IndexerTransaction> validated = transactionHandler.handleTransaction(indexerTransactions).toList().blockingGet();
			SystemTimer systemTimer = SystemTimer.create();
			systemTimer.start();
			utxoRepository.updateUtxoValidationStatus(validated.stream().map(IndexerTransaction::getTransaction).collect(Collectors.toList()), Coin.BCH)
					.blockingGet();
			logger.info("Completed utxo validation in time={}", systemTimer.getMsSinceStart());
		}
	}

	@Override
	public void notifyNewBestBlock(StoredBlock block) throws VerificationException {
		try {
			blockStore.put(block);
			logger.debug("Notified for height={} hash={}", block.getHeight(), block.getHeader().getHashAsString());
		} catch (Exception e) {
			logger.error("Could not put new block hash={}", block.getHeader().getHashAsString(), e);
		}
	}

	@Override
	public void reorganize(StoredBlock splitPoint,
			List<StoredBlock> oldBlocks,
			List<StoredBlock> newBlocks) throws VerificationException {

		logger.info("Reorg detected at split hash={} height={}",
				splitPoint.getHeader() != null ? splitPoint.getHeader().getHashAsString() : "",
				splitPoint.getHeight());

		for (StoredBlock oldBlock : oldBlocks) {
			com.bitcoin.indexer.blockchain.domain.Block block = converters.block(oldBlock.getHeader(),
					oldBlock.getHeight(), networkParameters);
			List<IndexerTransaction> oldTxs = block.getTransactions().stream()
					.map(IndexerTransaction::create)
					.collect(Collectors.toList());

			transactionHandler.reorganizeFromBlock(oldTxs).blockingGet();
		}

		for (StoredBlock newBlock : newBlocks) {
			consumeTransactions(converters.block(newBlock.getHeader(),
					newBlock.getHeight(),
					networkParameters).getTransactions());
		}
	}

	@Override
	public void onBlocksDownloaded(Block block, int height) {
		int blockStoreHeight = -1;
		try {
			blockStoreHeight = blockStore.getChainHead().getHeight();
		} catch (BlockStoreException e) {
		}

		if (height > 543374) {
			logger.info("Handling block={} height={} blockStoreHeight={}", block.getHash(), height, blockStoreHeight);
		}

		com.bitcoin.indexer.blockchain.domain.Block newBlock = converters.block(block, height, networkParameters);

		SystemTimer systemTimer = SystemTimer.create();
		systemTimer.start();

		if (isFullMode) {
			this.blockHandler.handleBlock(newBlock).blockingGet();
		}
		consumeTransactions(newBlock.getTransactions());

		if (height > 543374) {
			logger.info("Finished handling block={} height={} blockStoreHeight={} handlingTime={}", block.getHash(), height, blockStoreHeight, systemTimer.getMsSinceStart());
		}
	}

	private List<IndexerTransaction> createIndexerTransaction(List<com.bitcoin.indexer.blockchain.domain.Transaction> transactions, boolean isFullMode) {

		List<Input> inputs = transactions.stream()
				.filter(e -> !e.isSlp())
				.flatMap(e -> e.getInputs().stream()).collect(Collectors.toList());

		SystemTimer systemTimer = SystemTimer.create();
		systemTimer.start();
		List<Utxo> utxoList = List.of();
		if (!inputs.isEmpty()) {
			utxoList = utxoRepository.fetchUtxo(inputs, coin).blockingGet();
			logger.trace("Completed utxos={} fetch={}", utxoList.size(), systemTimer.getMsSinceStart());
		}

		List<IndexerTransaction> confirmedSlps = transactions.stream()
				.filter(com.bitcoin.indexer.blockchain.domain.Transaction::isSlp)
				.map(IndexerTransaction::create)
				.collect(Collectors.toList());
		if (utxoList.isEmpty() && confirmedSlps.isEmpty()) {
			return List.of();
		}

		Map<String, Utxo> txIdIndexToUtxo = utxoList.stream().collect(Collectors.toMap(e -> e.getTxId() + ":" + e.getIndex(), v -> v));
		Map<String, List<Input>> inputsWithValue = inputs.stream()
				.filter(e -> txIdIndexToUtxo.containsKey(e.getTxId() + ":" + e.getIndex()))
				.map(e -> {
					Utxo utxo = txIdIndexToUtxo.get(e.getTxId() + ":" + e.getIndex());
					return Input.knownValue(
							e.getAddress(),
							utxo.getAmount(),
							e.getIndex(),
							e.getTxId(),
							utxo.getSlpUtxo().orElse(null),
							e.isCoinbase(),
							e.getSequence()
					);
				}).collect(Collectors.groupingBy(Input::getTxId));

		List<IndexerTransaction> mightIncludeSlp = transactions.stream()
				.filter(transaction -> inputsWithValue.containsKey(transaction.getTxId()))
				.map(transaction -> {
					return IndexerTransaction.create(com.bitcoin.indexer.blockchain.domain.Transaction.create(
							transaction.getTxId(),
							transaction.getOutputs(),
							inputsWithValue.get(transaction.getTxId()),
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
							transaction.getBlockTime().orElse(null))
					);
				}).collect(Collectors.toList());
		confirmedSlps.addAll(mightIncludeSlp);
		return confirmedSlps;
	}

	public Flowable<com.bitcoin.indexer.blockchain.domain.Transaction> getTransactionFlowable() {
		return transactionFlowable;
	}

}
