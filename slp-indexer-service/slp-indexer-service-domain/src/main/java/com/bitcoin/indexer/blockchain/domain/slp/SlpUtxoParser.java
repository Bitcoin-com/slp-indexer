package com.bitcoin.indexer.blockchain.domain.slp;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.spongycastle.util.encoders.Hex;

import com.bitcoin.indexer.blockchain.domain.Utxo;

public class SlpUtxoParser {

	public static List<Utxo> slpParsedUtxo(SlpOpReturn slpOpReturn, List<Utxo> utxos, SlpTokenDetails slpTokenDetails, Integer height) {
		Map<Integer, Utxo> indexToUtxo = utxos.stream()
				.collect(Collectors.toMap(Utxo::getIndex, v -> v));

		if (slpOpReturn instanceof SlpOpReturnSend) {
			return sendUtxos((SlpOpReturnSend) slpOpReturn, utxos, slpTokenDetails, indexToUtxo, height);
		}

		if (slpOpReturn instanceof SlpOpReturnMint) {
			return mintSlpUtxos((SlpOpReturnMint) slpOpReturn, utxos, slpTokenDetails, indexToUtxo, height);
		}

		if (slpOpReturn instanceof SlpOpReturnGenesis) {
			return genesisUtxos((SlpOpReturnGenesis) slpOpReturn, utxos, slpTokenDetails, indexToUtxo, height);
		}
		return utxos;
	}

	//Parsing out the quantities and put them on the respective utxo
	private static List<Utxo> sendUtxos(SlpOpReturnSend slpOpReturn, List<Utxo> utxos, SlpTokenDetails slpTokenDetails, Map<Integer, Utxo> indexToUtxo, Integer height) {
		List<BigInteger> quantities = slpOpReturn.getQuantities();
		Set<Utxo> slpParsed = new HashSet<>();
		for (int i = 0; i < quantities.size(); i++) {
			Utxo utxo = indexToUtxo.get(i + 1); //0 is always an OP_RETURN in our utxos list
			if (utxo == null) {
				continue;
			}
			BigInteger amount = quantities.get(i);
			SlpUtxo slpUtxo = SlpUtxo.send(slpOpReturn.getTokenId(),
					amountWithDecimals(slpTokenDetails, new BigDecimal(amount)),
					slpTokenDetails.getTicker(), slpTokenDetails.getName(),
					slpOpReturn.getTokenType().getType(),
					Hex.toHexString(slpOpReturn.getTokenType().getBytes()));

			slpParsed.add(Utxo.create(utxo.getTxId(),
					utxo.getAddress(),
					utxo.getScriptPubkey(),
					utxo.getAmount(),
					utxo.isConfirmed(),
					utxo.getIndex(),
					utxo.isSpent(),
					utxo.getTimestamp(),
					slpUtxo, true,
					height
			));
		}
		slpParsed.addAll(utxos);
		List<Utxo> result = new ArrayList<>(slpParsed);
		result.sort(Comparator.comparing(Utxo::getIndex));
		return result;
	}

	private static List<Utxo> genesisUtxos(SlpOpReturnGenesis slpOpReturn, List<Utxo> utxos, SlpTokenDetails slpTokenDetails, Map<Integer, Utxo> indexToUtxo, Integer height) {
		Integer batonVout = slpOpReturn.getBatonVout();
		Set<Utxo> slpParsed = new HashSet<>();

		if (batonVout != null) { //A genesis might have a baton or not
			Utxo batonUtxo = indexToUtxo.get(batonVout);
			if (batonUtxo != null) { //Some incorrect SLP txs points a batonVout utxo that does not exist
				Utxo baton = Utxo.create(batonUtxo.getTxId(),
						batonUtxo.getAddress(),
						batonUtxo.getScriptPubkey(),
						batonUtxo.getAmount(),
						batonUtxo.isConfirmed(),
						batonUtxo.getIndex(),
						batonUtxo.isSpent(),
						batonUtxo.getTimestamp(),
						SlpUtxo.genesis(slpOpReturn.getTokenId(),
								slpOpReturn.getTicker(),
								BigDecimal.ZERO,
								true,
								slpTokenDetails.getName(),
								slpOpReturn.getTokenType().getType(),
								Hex.toHexString(slpOpReturn.getTokenType().getBytes())),
						true,
						height);
				slpParsed.add(baton);
			}
		}

		SlpUtxo genesis = SlpUtxo.genesis(slpOpReturn.getTokenId(), slpOpReturn.getTicker(), amountWithDecimals(slpTokenDetails,
				new BigDecimal(slpOpReturn.getMintedAmount())), false, slpTokenDetails.getName(), slpOpReturn.getTokenType().getType(), Hex.toHexString(slpOpReturn.getTokenType().getBytes()));
		Utxo genesisUtxo = indexToUtxo.get(1); //Initial mint quantity is always on vout 1
		Utxo minted = Utxo.create(genesisUtxo.getTxId(),
				genesisUtxo.getAddress(),
				genesisUtxo.getScriptPubkey(),
				genesisUtxo.getAmount(),
				genesisUtxo.isConfirmed(),
				genesisUtxo.getIndex(),
				genesisUtxo.isSpent(),
				genesisUtxo.getTimestamp(),
				genesis, true,
				height);
		slpParsed.add(minted);
		slpParsed.addAll(utxos);
		List<Utxo> result = new ArrayList<>(slpParsed);
		result.sort(Comparator.comparing(Utxo::getIndex));
		return result;
	}

	private static List<Utxo> mintSlpUtxos(SlpOpReturnMint slpOpReturn, List<Utxo> utxos, SlpTokenDetails slpTokenDetails, Map<Integer, Utxo> indexToUtxo, Integer height) {
		BigInteger mintedAmount = slpOpReturn.getMintedAmount();
		Integer batonVout = slpOpReturn.getBatonVout();
		Set<Utxo> slpParsed = new HashSet<>();
		if (batonVout != null && indexToUtxo.containsKey(batonVout)) { //Is baton passed on to the utxos if so add it
			Utxo batonUtxo = indexToUtxo.get(batonVout);
			Utxo baton = Utxo.create(batonUtxo.getTxId(),
					batonUtxo.getAddress(),
					batonUtxo.getScriptPubkey(),
					batonUtxo.getAmount(),
					batonUtxo.isConfirmed(),
					batonUtxo.getIndex(),
					batonUtxo.isSpent(),
					batonUtxo.getTimestamp(),
					SlpUtxo.mint(slpOpReturn.getTokenId(), BigDecimal.ZERO, true, slpTokenDetails.getTicker(), slpTokenDetails.getName(), slpOpReturn.getTokenType().getType(), Hex.toHexString(slpOpReturn.getTokenType().getBytes())),
					true,
					height);
			slpParsed.add(baton);
		}
		SlpUtxo mintedSlp = SlpUtxo.mint(slpOpReturn.getTokenId(),
				amountWithDecimals(slpTokenDetails, new BigDecimal(mintedAmount)),
				false,
				slpTokenDetails.getTicker(),
				slpTokenDetails.getName(),
				slpOpReturn.getTokenType().getType(),
				Hex.toHexString(slpOpReturn.getTokenType().getBytes()));
		Utxo mintedUtxo = indexToUtxo.get(1); //Additional token quantities is always on vout 1
		Utxo minted = Utxo.create(mintedUtxo.getTxId(),
				mintedUtxo.getAddress(),
				mintedUtxo.getScriptPubkey(),
				mintedUtxo.getAmount(),
				mintedUtxo.isConfirmed(),
				mintedUtxo.getIndex(),
				mintedUtxo.isSpent(),
				mintedUtxo.getTimestamp(),
				mintedSlp, true,
				height);
		slpParsed.add(minted);
		slpParsed.addAll(utxos);
		List<Utxo> result = new ArrayList<>(slpParsed);
		result.sort(Comparator.comparing(Utxo::getIndex));
		return result;
	}

	public static boolean isGenesis(SlpOpReturn slpOpReturn) {
		return slpOpReturn instanceof SlpOpReturnGenesis;
	}

	public static boolean isMint(SlpOpReturn slpOpReturn) {
		return slpOpReturn instanceof SlpOpReturnMint;
	}

	public static boolean isSent(SlpOpReturn slpOpReturn) {
		return slpOpReturn instanceof SlpOpReturnSend;
	}

	//Preparse the decimals
	private static BigDecimal amountWithDecimals(SlpTokenDetails slpTokenDetails, BigDecimal value) {
		if (slpTokenDetails.getDecimals() == 0) {
			return value;
		}
		return value.movePointLeft(slpTokenDetails.getDecimals());
	}
}
