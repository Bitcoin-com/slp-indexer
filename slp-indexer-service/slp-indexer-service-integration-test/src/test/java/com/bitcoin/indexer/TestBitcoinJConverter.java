package com.bitcoin.indexer;

import java.util.List;

import org.springframework.data.util.Pair;

import com.bitcoin.indexer.blockchain.domain.Address;
import com.bitcoin.indexer.blockchain.domain.Block;
import com.bitcoin.indexer.blockchain.domain.Input;
import com.bitcoin.indexer.blockchain.domain.Transaction;
import com.bitcoin.indexer.blockchain.domain.Utxo;
import com.bitcoin.indexer.blockchain.domain.UtxoMinimalData;
import com.bitcoin.indexer.blockchain.domain.slp.ExtendedDetails;
import com.bitcoin.indexer.blockchain.domain.slp.SlpTokenDetails;
import com.bitcoin.indexer.blockchain.domain.slp.SlpTokenId;
import com.bitcoin.indexer.blockchain.domain.slp.SlpValid.Valid;
import com.bitcoin.indexer.core.Coin;
import com.bitcoin.indexer.facade.BitcoinJConverters;
import com.bitcoin.indexer.repository.BlockRepository;
import com.bitcoin.indexer.repository.SlpDetailsRepository;
import com.bitcoin.indexer.repository.UtxoRepository;

import io.reactivex.Maybe;
import io.reactivex.Single;

public class TestBitcoinJConverter {

	public static BitcoinJConverters bitcoinJConverters = new BitcoinJConverters(new SlpDetailsRepository() {
		@Override
		public Maybe<SlpTokenDetails> fetchSlpDetails(SlpTokenId slpTokenId) {
			return Maybe.empty();
		}

		@Override
		public Single<List<SlpTokenDetails>> fetchSlpDetails(List<SlpTokenId> slpTokenIds) {
			return Single.just(List.of());
		}

		@Override
		public Single<SlpTokenDetails> saveSlpTokenDetails(SlpTokenDetails slpTokenDetails, Integer lastActiveMint, Integer lastActiveSend, Integer blockCreated) {
			return Single.just(slpTokenDetails);
		}

		@Override
		public Maybe<SlpTokenDetails> updateMetadata(SlpTokenId slpTokenId, Integer lastActiveMint, Integer lastActiveSend) {
			return fetchSlpDetails(slpTokenId);
		}

		@Override
		public Single<List<ExtendedDetails>> fetchExtendedDetails(List<SlpTokenId> slpTokenIds) {
			return Single.just(List.of());
		}

	}, new UtxoRepository() {
		@Override
		public Single<List<Utxo>> fetchUtxosFromAddress(Address address, Coin coin, boolean useCache, Valid parentValid) {
			return Single.just(List.of());
		}

		@Override
		public Single<List<Utxo>> fetchUtxosFromAddressTokenId(Address address, String tokenId, Coin coin, boolean useCache, Valid parentValidation) {
			return null;
		}

		@Override
		public Single<List<Utxo>> fetchSlpUtxosForAddress(Address address, Coin coin, boolean useCache, Valid parentValid) {
			return null;
		}

		@Override
		public Maybe<Utxo> fetchUtxo(String txId, int inputIndex, Coin coin) {
			return Maybe.empty();

		}

		@Override
		public Single<List<Utxo>> fetchUtxosWithTokenId(List<String> tokenIds, boolean isSpent, Valid parentValid) {
			return null;
		}

		@Override
		public Single<List<UtxoMinimalData>> fetchMinimalUtxoData(List<String> tokenIds, boolean isSpent, Valid parentValidation) {
			return null;
		}

		@Override
		public Single<List<Utxo>> fetchUtxos(List<Pair<String, Integer>> txIdIndexs, Coin coin) {
			return null;
		}

		@Override
		public Single<List<Utxo>> fetchUtxo(List<Input> inputs, Coin coin) {
			return null;
		}

		@Override
		public Single<List<Utxo>> fetchUtxoNoCache(List<Input> inputs, Coin coin) {
			return null;
		}

		@Override
		public Single<List<Utxo>> fetchUtxos(String txId, Coin coin) {
			return Single.just(List.of());
		}

		@Override
		public Single<List<Utxo>> saveUtxo(List<Utxo> utxos, Coin coin) {
			return null;
		}

		@Override
		public Single<List<Utxo>> spendUtxo(List<Utxo> utxos, Coin coin) {
			return null;
		}

		@Override
		public Single<List<Utxo>> removeUtxo(List<Utxo> utxos, Coin coin) {
			return null;
		}

		@Override
		public Single<List<Transaction>> updateUtxoValidationStatus(List<Transaction> txs, Coin coin) {
			return null;
		}

		@Override
		public void addToCache(Utxo utxo) {

		}
	},
			Coin.BCH
			, new BlockRepository() {
		@Override
		public Single<Block> saveBlock(Block block) {
			return null;
		}

		@Override
		public Single<Long> saveHeight(Long currentHeight) {
			return null;
		}

		@Override
		public Single<Long> currentHeight() {
			return null;
		}

		@Override
		public Maybe<Block> getBlock(String hash) {
			return null;
		}
	}
	);
}
