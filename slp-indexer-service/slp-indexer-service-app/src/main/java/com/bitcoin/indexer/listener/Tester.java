package com.bitcoin.indexer.listener;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.Transaction.SigHash;
import org.bitcoinj.core.TransactionInput;
import org.bitcoinj.core.TransactionOutPoint;
import org.bitcoinj.crypto.ChildNumber;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.crypto.HDKeyDerivation;
import org.bitcoinj.crypto.MnemonicCode;
import org.bitcoinj.crypto.TransactionSignature;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptBuilder;
import org.spongycastle.util.encoders.Hex;

import com.bitcoin.indexer.blockchain.domain.slp.SlpOpReturnSend;
import com.bitcoin.indexer.blockchain.domain.slp.SlpTokenId;
import com.bitcoin.indexer.blockchain.domain.slp.SlpTokenType;

public class Tester {
	public static void main(String[] args) {

		List<String> mnemonic = Arrays.asList("genius antique dance fault relief walnut soul scrap muffin slam thumb movie".split(" "));

		DeterministicKey masterPrivateKey = HDKeyDerivation.createMasterPrivateKey(MnemonicCode.toSeed(mnemonic, ""));

		DeterministicKey derived = masterPrivateKey.derive(44).derive(245).derive(0);

		derived = HDKeyDerivation.deriveChildKey(HDKeyDerivation.deriveChildKey(
				derived, new ChildNumber(0, false)), new ChildNumber(0, false));
		Address address = Address.fromBase58(MainNetParams.get(), derived.toAddress(MainNetParams.get()).toBase58());

		BigInteger privKey = derived.getPrivKey();

		//simpleledger:qzqs3uky8hpdk20094whm8rgfj4jkkwklqxr6gkx0q
		//bitcoincash:qzqs3uky8hpdk20094whm8rgfj4jkkwklq2c3nrx37
		Script script = new SlpOpReturnSend(SlpTokenType.PERMISSIONLESS,
				new SlpTokenId("4de69e374a8ed21cbddd47f2338cc0f479dc58daa2bbe11cd604ca488eca0ddf"),
				List.of(new BigInteger("900"))
		).createScript();

		Transaction transaction = new Transaction(MainNetParams.get());
		byte[] inputScript = new ScriptBuilder()
				.data(Hex.decode("76a9148108f2c43dc2db29ef2d5d7d9c684cab2b59d6f888ac"))
				.build().getProgram();
		String txId = "21fac6723867eaf294d5b62445c94a031329560ebf32265aea9db8a6e1e7a91a";

		TransactionInput slpInput = new TransactionInput(
				MainNetParams.get(), transaction, inputScript,
				new TransactionOutPoint(MainNetParams.get(), 1, Sha256Hash.wrap(txId)),
				org.bitcoinj.core.Coin.valueOf(546));

		byte[] valScript = new ScriptBuilder()
				.data(Hex.decode("76a9148108f2c43dc2db29ef2d5d7d9c684cab2b59d6f888ac"))
				.build().getProgram();

		TransactionInput value = new TransactionInput(
				MainNetParams.get(), transaction, valScript,
				new TransactionOutPoint(MainNetParams.get(), 0, Sha256Hash.wrap("1a1a558cb64cb7544618002b7b3420fe2ad9a78a62b77c955bd0826e0014d08e")),
				org.bitcoinj.core.Coin.valueOf(1000));

		transaction.addInput(slpInput);
		transaction.addInput(value);

		transaction.addOutput(org.bitcoinj.core.Coin.ZERO, script);
		transaction.addOutput(Transaction.MIN_NONDUST_OUTPUT, Address.fromBase58(MainNetParams.get(), "18RMr3aNfybnfA5Wyd7amjzN4eqRD1W13Q"));
		transaction.addOutput(org.bitcoinj.core.Coin.valueOf(546), Address.fromBase58(MainNetParams.get(), "18RMr3aNfybnfA5Wyd7amjzN4eqRD1W13Q"));

		for (int i = 0; i < transaction.getInputs().size(); i++) {
			TransactionInput input = transaction.getInputs().get(i);
			ECKey key = ECKey.fromPrivate(privKey);
			TransactionSignature signature = transaction.calculateWitnessSignature(
					i,
					key,
					input.getScriptSig().getChunks().get(0).data,
					input.getValue(), SigHash.ALL, false);
			input.setScriptSig(new ScriptBuilder()
					.data(signature.encodeToBitcoin())
					.data(key.getPubKeyPoint().getEncoded(true))
					.build());
		}

		System.out.println(Hex.toHexString(transaction.bitcoinSerialize()));
	}
}
