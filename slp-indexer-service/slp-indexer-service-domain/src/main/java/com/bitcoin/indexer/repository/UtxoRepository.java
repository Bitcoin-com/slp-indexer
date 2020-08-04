package com.bitcoin.indexer.repository;

import java.util.List;

import org.springframework.data.util.Pair;

import com.bitcoin.indexer.blockchain.domain.Address;
import com.bitcoin.indexer.blockchain.domain.Input;
import com.bitcoin.indexer.blockchain.domain.Transaction;
import com.bitcoin.indexer.blockchain.domain.Utxo;
import com.bitcoin.indexer.blockchain.domain.UtxoMinimalData;
import com.bitcoin.indexer.blockchain.domain.slp.SlpValid.Valid;
import com.bitcoin.indexer.core.Coin;

import io.reactivex.Maybe;
import io.reactivex.Single;

public interface UtxoRepository {

	Single<List<Utxo>> fetchUtxosFromAddress(Address address, Coin coin, boolean useCache, Valid parentValidation);

	Single<List<Utxo>> fetchUtxosFromAddressTokenId(Address address, String tokenId, Coin coin, boolean useCache, Valid parentValidation);

	Single<List<Utxo>> fetchSlpUtxosForAddress(Address address, Coin coin, boolean useCache, Valid parentValidation);

	Maybe<Utxo> fetchUtxo(String txId, int inputIndex, Coin coin);

	Single<List<Utxo>> fetchUtxosWithTokenId(List<String> tokenIds, boolean isSpent, Valid parentValidation);

	Single<List<UtxoMinimalData>> fetchMinimalUtxoData(List<String> tokenIds, boolean isSpent, Valid parentValidation);

	Single<List<Utxo>> fetchUtxos(List<Pair<String, Integer>> txIdIndexs, Coin coin);

	Single<List<Utxo>> fetchUtxo(List<Input> inputs, Coin coin);

	Single<List<Utxo>> fetchUtxoNoCache(List<Input> inputs, Coin coin);

	Single<List<Utxo>> fetchUtxos(String txId, Coin coin);

	//Internal use apis
	Single<List<Utxo>> saveUtxo(List<Utxo> utxos, Coin coin);

	Single<List<Utxo>> spendUtxo(List<Utxo> utxos, Coin coin);

	Single<List<Utxo>> removeUtxo(List<Utxo> utxos, Coin coin);

	Single<List<Transaction>> updateUtxoValidationStatus(List<Transaction> txs, Coin coin);

	void addToCache(Utxo utxo);

}
