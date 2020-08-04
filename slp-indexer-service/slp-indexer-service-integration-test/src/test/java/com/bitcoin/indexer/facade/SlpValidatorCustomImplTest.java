package com.bitcoin.indexer.facade;

import static org.hamcrest.MatcherAssert.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bitcoinj.core.Transaction;
import org.bitcoinj.params.MainNetParams;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.spongycastle.util.encoders.Hex;

import com.bitcoin.indexer.FakeBlockRepo;
import com.bitcoin.indexer.FakeInMemoryDetails;
import com.bitcoin.indexer.FakeUtxoRepository;
import com.bitcoin.indexer.blockchain.domain.Address;
import com.bitcoin.indexer.blockchain.domain.IndexerTransaction;
import com.bitcoin.indexer.blockchain.domain.slp.SlpOpReturn;
import com.bitcoin.indexer.blockchain.domain.slp.SlpTokenDetails;
import com.bitcoin.indexer.blockchain.domain.slp.SlpTokenId;
import com.bitcoin.indexer.blockchain.domain.slp.SlpValid;
import com.bitcoin.indexer.facade.validators.GenesisValidatorAssumeParentValid;
import com.bitcoin.indexer.facade.validators.MintValidatorAssumeParentValid;
import com.bitcoin.indexer.facade.validators.SendValidatorAssumeParentValid;
import com.bitcoin.indexer.facade.validators.SlpValidatorCustomImplAssumeParentValid;
import com.bitcoin.indexer.facade.validators.SlpValidatorFacade;
import com.bitcoin.indexer.repository.TransactionRepository;
import com.bitcoin.indexer.core.Coin;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Single;

@Ignore
public class SlpValidatorCustomImplTest {

	private Map<String, IndexerTransaction> inMemory = new HashMap<>();
	private BitcoinJConverters bitcoinJConverters;
	private FakeInMemoryDetails fakeInMemoryDetails;
	private SlpValidatorFacade validatorFacade;
	private FakeUtxoRepository fakeUtxoRepository;

	private TransactionRepository transactionRepository = new TransactionRepository() {

		@Override
		public Maybe<IndexerTransaction> fetchTransaction(String txId, Coin coin, boolean useCache) {
			if (inMemory.containsKey(txId)) {
				return Maybe.just(inMemory.get(txId));
			}
			return Maybe.empty();
		}

		@Override
		public Single<List<IndexerTransaction>> fetchTransactions(List<String> txIds, Coin coin, boolean useCache) {
			return null;
		}

		@Override
		public Single<List<IndexerTransaction>> fetchTransactionsInvolvingToken(String tokenId, boolean useCache, int page) {
			return null;
		}

		@Override
		public Single<List<String>> fetchTransactionIdsByAddress(String address, Coin coin) {
			return null;
		}

		@Override
		public Single<List<IndexerTransaction>> fetchTransactions(Address address, Coin coin) {
			return Single.just(List.of());
		}

		@Override
		public Single<List<IndexerTransaction>> fetchValidTransactions(List<String> txIds, Coin coin, boolean useCache) {
			return null;
		}

		@Override
		public Single<BigDecimal> transactionsForTokenId(String tokenId) {
			return null;
		}

		@Override
		public Single<Map<String, BigDecimal>> transactionsForTokenIds(List<String> tokenIds) {
			return null;
		}

		@Override
		public Flowable<IndexerTransaction> fetchTransactions(Integer height, String tokenId, int page, Coin coin) {
			return null;
		}

		@Override
		public Single<List<IndexerTransaction>> saveTransaction(List<IndexerTransaction> indexerTransaction) {

			indexerTransaction.forEach(i -> {
				inMemory.put(i.getTransaction().getTxId(), i);
			});
			return Single.just(indexerTransaction);
		}

		@Override
		public Completable handleReorg(List<IndexerTransaction> oldTxs) {
			return null;
		}
	};

	@Before
	public void setup() {
		inMemory.clear();
		fakeUtxoRepository = new FakeUtxoRepository();
		validatorFacade = new SlpValidatorCustomImplAssumeParentValid(transactionRepository,
				new MintValidatorAssumeParentValid(transactionRepository, fakeUtxoRepository),
				new SendValidatorAssumeParentValid(transactionRepository, fakeUtxoRepository),
				new GenesisValidatorAssumeParentValid(transactionRepository, fakeUtxoRepository));
		fakeInMemoryDetails = new FakeInMemoryDetails();
		bitcoinJConverters = new BitcoinJConverters(fakeInMemoryDetails, new FakeUtxoRepository(), Coin.BCH, new FakeBlockRepo());
	}

	@Test
	public void genesis_always_valid() {
		Transaction transaction = new Transaction(MainNetParams.get(), Hex.decode(slpGenesis()));
		com.bitcoin.indexer.blockchain.domain.Transaction genesis = bitcoinJConverters.transaction(transaction, MainNetParams.get(), 0, false, "", Instant.ofEpochMilli(1000), Instant.ofEpochMilli(1000));
		SlpOpReturn slpOpReturn = genesis.getSlpOpReturn().get(0);
		SlpValid valid = validatorFacade.isValid(genesis.getTxId(), slpOpReturn.getTokenId().getHex(), slpOpReturn.getTokenType().getType(), genesis.getOutputs(), genesis.getInputs(), validatorFacade);
		assertThat(valid.getValid(), Matchers.is(SlpValid.Valid.VALID));
	}

	@Test
	public void transaction_after_genesis_valid() {
		Transaction transaction = new Transaction(MainNetParams.get(), Hex.decode(slpGenesis()));
		com.bitcoin.indexer.blockchain.domain.Transaction genesis = bitcoinJConverters.transaction(transaction, MainNetParams.get(), 0, false, "", Instant.ofEpochMilli(1000), Instant.ofEpochMilli(1000));
		inMemory.put(genesis.getTxId(), IndexerTransaction.create(genesis));
		fakeUtxoRepository.saveUtxo(genesis.getOutputs(), Coin.BCH).blockingGet();

		Transaction sendAfterGenesis = new Transaction(MainNetParams.get(), Hex.decode(slpSendAfterGenesis()));
		com.bitcoin.indexer.blockchain.domain.Transaction send = bitcoinJConverters.transaction(sendAfterGenesis, MainNetParams.get(), 0, false, "", Instant.ofEpochMilli(1000), Instant.ofEpochMilli(1000));
		SlpOpReturn slpOpReturn = genesis.getSlpOpReturn().get(0);
		SlpValid valid = validatorFacade.isValid(send.getTxId(), slpOpReturn.getTokenId().getHex(), slpOpReturn.getTokenType().getType(), send.getOutputs(), send.getInputs(), validatorFacade);

		assertThat(valid.getValid(), Matchers.is(SlpValid.Valid.VALID));
	}

	//24780511962bdf6f514b1b4181ffabf6b970f9c2f65e5609c5208230d276e08e
	@Test
	public void transaction_should_be_valid() {
		SlpTokenDetails slpTokenDetails = new SlpTokenDetails(new SlpTokenId("4abbea22956e7db07ac3ae7eb88b14f23ccc5dce4273728275cb17ec91e6f57c"), "", "", 8, "", null);
		fakeInMemoryDetails.saveSlpTokenDetails(slpTokenDetails, null, null, 1244).blockingGet();

		Transaction errorTx = new Transaction(MainNetParams.get(), Hex.decode(
				"0200000002a1ce09c05353adc011ed853838f53926e67ac2cd797a3e9adafd3a392646e8f7020000006a473044022044181891cd4eb8c718eaa7de3dd9b16ec6897e84daf44f91ce528008a76f261702205755196a9d200a1650aaba418c7b6e03752b4eabf3aabaebc5424f8d569f7e2e412103c782f3fb67aced9a921ff1eedee2bbbc0c989952cc4072ff8c77fa5f62eab5aeffffffffa1ce09c05353adc011ed853838f53926e67ac2cd797a3e9adafd3a392646e8f7030000006b48304502210085cebe0410abb78dda22f1bbc9b8ace47dc7e944434fcf815418c0ebad920fb302204ef4209010fad1128f4bc445a4437b79dd0d4ece86c20e4ceb13750bed94f80d4121032a597661f7ba57b04ec0ce514b3fe5a411f4ece81f589c0cedfc36e32fbb85bfffffffff040000000000000000406a04534c500001010453454e44204abbea22956e7db07ac3ae7eb88b14f23ccc5dce4273728275cb17ec91e6f57c08000000000003d09008000000024e0a145022020000000000001976a914bd59d42cdeb398c76eb9284cdcbd262eb8c1176c88ac22020000000000001976a914b9d65fc863a411237d81c4fc347ab0f9e002889388acd83b3d00000000001976a914a15c23a82beec19cb92ba1ea8506f6a322f22a0f88ac00000000"));
		BitcoinJConverters alwaysValid = new BitcoinJConverters(fakeInMemoryDetails, new FakeUtxoRepository(), Coin.BCH, new FakeBlockRepo());

		Transaction validParent = new Transaction(MainNetParams.get(), Hex.decode(
				"0200000002b7a4a67a3ea33643d2d872e412ec9f9a581bf31062a616ba04931593fca16199020000006a47304402203063308c46b220da10192ac965a2cab90f7612b6bb2e14e1cd99dfb719182612022071a2874c7e10953d6deefaa68e4bb12b18040195c603d7c113ddf644150586ad412103c782f3fb67aced9a921ff1eedee2bbbc0c989952cc4072ff8c77fa5f62eab5aeffffffff3e67c08f8c3f0da6828d93daaf8a5a3dd96d6ed2c7a2335f11c1d37f463a2bb5000000006a473044022061075d162b32f071a116946af871b308fdd5edfd6aba3af49490c15841b827d3022054121ef8b30fce77d4c2a76d9b04a16bc7fda6c6e582116cf13ad48062e36b454121032a597661f7ba57b04ec0ce514b3fe5a411f4ece81f589c0cedfc36e32fbb85bfffffffff040000000000000000406a04534c500001010453454e44204abbea22956e7db07ac3ae7eb88b14f23ccc5dce4273728275cb17ec91e6f57c08000000000003d09008000000024e0de4e022020000000000001976a9147aa062a2750964b42c7ee7a3f9cde9c87256aa8888ac22020000000000001976a914b9d65fc863a411237d81c4fc347ab0f9e002889388acfe3f3d00000000001976a914a15c23a82beec19cb92ba1ea8506f6a322f22a0f88ac00000000"));

		com.bitcoin.indexer.blockchain.domain.Transaction validChildConverted = alwaysValid.transaction(validParent, MainNetParams.get(), 1, false, null, Instant.now(), null);
		IndexerTransaction value = IndexerTransaction.create(validChildConverted);
		inMemory.put(validChildConverted.getTxId(), value);
		fakeUtxoRepository.saveUtxo(value.getTransaction().getOutputs(), Coin.BCH).blockingGet();

		com.bitcoin.indexer.blockchain.domain.Transaction tx = this.bitcoinJConverters.transaction(errorTx, MainNetParams.get(), 1, false, null, Instant.now(), null);

		SlpOpReturn slpOpReturn = tx.getSlpOpReturn().get(0);
		SlpValid valid = validatorFacade.isValid(tx.getTxId(), slpOpReturn.getTokenId().getHex(), slpOpReturn.getTokenType().getType(), tx.getOutputs(), tx.getInputs(), validatorFacade);

		assertThat(valid.getValid(), Matchers.is(SlpValid.Valid.VALID));
	}

	//VERY HARD TO SETUP CORRECTLY
	@Ignore
	public void transaction_after_send_valid() {
		Transaction transaction = new Transaction(MainNetParams.get(), Hex.decode(slpGenesis()));
		com.bitcoin.indexer.blockchain.domain.Transaction genesis = bitcoinJConverters.transaction(transaction, MainNetParams.get(), 0, false, "", Instant.ofEpochMilli(1000), Instant.ofEpochMilli(1000));
		inMemory.put(genesis.getTxId(), IndexerTransaction.create(genesis));

		Transaction sendAfterGenesis = new Transaction(MainNetParams.get(), Hex.decode(slpSendAfterGenesis()));
		com.bitcoin.indexer.blockchain.domain.Transaction send = bitcoinJConverters.transaction(sendAfterGenesis, MainNetParams.get(), 0, false, "", Instant.ofEpochMilli(1000), Instant.ofEpochMilli(1000));
		inMemory.put(send.getTxId(), IndexerTransaction.create(send));

		Transaction sendLinked = new Transaction(MainNetParams.get(), Hex.decode(previousSend()));
		com.bitcoin.indexer.blockchain.domain.Transaction linked = bitcoinJConverters.transaction(sendLinked, MainNetParams.get(), 0, false, "", Instant.ofEpochMilli(1000), Instant.ofEpochMilli(1000));
		inMemory.put(linked.getTxId(), IndexerTransaction.create(linked));

		Transaction sendAfterSend = new Transaction(MainNetParams.get(), Hex.decode(slpSendAfterSend()));
		com.bitcoin.indexer.blockchain.domain.Transaction converted = bitcoinJConverters.transaction(sendAfterSend, MainNetParams.get(), 0, false, "", Instant.ofEpochMilli(1000), Instant.ofEpochMilli(1000));

		assertThat(converted.getSlpValid().get().getValid(), Matchers.is(SlpValid.Valid.VALID));
	}

	@Test
	public void transaction_after_mint_valud() {
		Transaction transaction = new Transaction(MainNetParams.get(), Hex.decode((withBaton())));
		com.bitcoin.indexer.blockchain.domain.Transaction withBaton = bitcoinJConverters.transaction(transaction, MainNetParams.get(), 0, false, "", Instant.ofEpochMilli(1000), Instant.ofEpochMilli(1000));
		inMemory.put(withBaton.getTxId(), IndexerTransaction.create(withBaton));
		fakeUtxoRepository.saveUtxo(withBaton.getOutputs(), Coin.BCH).blockingGet();

		Transaction pr = new Transaction(MainNetParams.get(), Hex.decode((previous())));
		com.bitcoin.indexer.blockchain.domain.Transaction prev = bitcoinJConverters.transaction(pr, MainNetParams.get(), 0, false, "", Instant.ofEpochMilli(1000), Instant.ofEpochMilli(1000));
		inMemory.put(prev.getTxId(), IndexerTransaction.create(prev));
		fakeUtxoRepository.saveUtxo(prev.getOutputs(), Coin.BCH).blockingGet();

		Transaction m = new Transaction(MainNetParams.get(), Hex.decode((mint())));
		com.bitcoin.indexer.blockchain.domain.Transaction minted = bitcoinJConverters.transaction(m, MainNetParams.get(), 0, false, "", Instant.ofEpochMilli(1000), Instant.ofEpochMilli(1000));

		SlpOpReturn slpOpReturn = minted.getSlpOpReturn().get(0);
		SlpValid valid = validatorFacade.isValid(minted.getTxId(), slpOpReturn.getTokenId().getHex(), slpOpReturn.getTokenType().getType(), minted.getOutputs(), minted.getInputs(), validatorFacade);

		assertThat(valid.getValid(), Matchers.is(SlpValid.Valid.VALID));
	}

	//f58c7bb144f24385c341687c725c095af274fe16666b2903b6e061f18d895ec5
	@Test
	public void should_be_valid_mint() {
		Transaction transaction = new Transaction(MainNetParams.get(), Hex.decode(
				"010000000212cf9afb0dc0e49ef2cb3e8198d9dff3edeac178bd5f0d0c250cb35aeee9f3e0030000006b4830450221009c028bd297d6e5f605cb8f4620cda81b74350e9b0d73e48fff437808ff8ce02c02203bdb6d2470971cf7a6e5f020be5266f0a7a6c8f8774b252bf0fa14d54cc72390412102a97f90ed744a242f59d8f1abcbcf645938ddce3eb555ee3d6cb8586fe2bdd4e9feffffff12cf9afb0dc0e49ef2cb3e8198d9dff3edeac178bd5f0d0c250cb35aeee9f3e0020000006a47304402201abcb53157d8839aff3465aa02575fdfb4d00454d1b06c8d60dde9510632288c02206e15e99a169a776b915ce2a7398282f239c0bdf9bafbbf988984390cd43ca27e412103b0d69892bf7cc61dee72c2a099820674e368514ea0fdc42b553c3fdf9dd7987efeffffff040000000000000000396a04534c50000101044d494e5420e0f3e9ee5ab30c250c0d5fbd78c1eaedf3dfd998813ecbf29ee4c00dfb9acf120102080000000005f6113922020000000000001976a9149935fe85093fc845b1df7e04593d60ea745ab90b88ac22020000000000001976a9149935fe85093fc845b1df7e04593d60ea745ab90b88ac896e0200000000001976a91477ed2f507ac1cc1affe74d75c1a3612e6b6d7ff188aca74b0800"));
		com.bitcoin.indexer.blockchain.domain.Transaction first = bitcoinJConverters.transaction(transaction, MainNetParams.get(), 0, false, "", Instant.ofEpochMilli(1000), Instant.ofEpochMilli(1000));
		inMemory.put(first.getTxId(), IndexerTransaction.create(first).withValid(SlpValid.valid("Valid")));
		fakeUtxoRepository.saveUtxo(first.getOutputs(), Coin.BCH).blockingGet();

		Transaction pr = new Transaction(MainNetParams.get(), Hex.decode(
				"010000000212cf9afb0dc0e49ef2cb3e8198d9dff3edeac178bd5f0d0c250cb35aeee9f3e0030000006b4830450221009c028bd297d6e5f605cb8f4620cda81b74350e9b0d73e48fff437808ff8ce02c02203bdb6d2470971cf7a6e5f020be5266f0a7a6c8f8774b252bf0fa14d54cc72390412102a97f90ed744a242f59d8f1abcbcf645938ddce3eb555ee3d6cb8586fe2bdd4e9feffffff12cf9afb0dc0e49ef2cb3e8198d9dff3edeac178bd5f0d0c250cb35aeee9f3e0020000006a47304402201abcb53157d8839aff3465aa02575fdfb4d00454d1b06c8d60dde9510632288c02206e15e99a169a776b915ce2a7398282f239c0bdf9bafbbf988984390cd43ca27e412103b0d69892bf7cc61dee72c2a099820674e368514ea0fdc42b553c3fdf9dd7987efeffffff040000000000000000396a04534c50000101044d494e5420e0f3e9ee5ab30c250c0d5fbd78c1eaedf3dfd998813ecbf29ee4c00dfb9acf120102080000000005f6113922020000000000001976a9149935fe85093fc845b1df7e04593d60ea745ab90b88ac22020000000000001976a9149935fe85093fc845b1df7e04593d60ea745ab90b88ac896e0200000000001976a91477ed2f507ac1cc1affe74d75c1a3612e6b6d7ff188aca74b0800"));
		com.bitcoin.indexer.blockchain.domain.Transaction prev = bitcoinJConverters.transaction(pr, MainNetParams.get(), 0, false, "", Instant.ofEpochMilli(1000), Instant.ofEpochMilli(1000));
		inMemory.put(prev.getTxId(), IndexerTransaction.create(prev).withValid(SlpValid.valid("Valid")));
		fakeUtxoRepository.saveUtxo(prev.getOutputs(), Coin.BCH).blockingGet();

		Transaction m = new Transaction(MainNetParams.get(), Hex.decode(
				"01000000021c3a4cfc973e05ccee521c597d3a0f49d84915e37e25e37eaba26ffeb6c52b66030000006a47304402203dcf1198fd36ff2d12f77089ab27041acec3d3ae1fcc4d684bc106da10da05c502201349891497d44dedd30e1c5e5fdd9c9aa31eaeb86e171132ced53bd8ac9024e9412103c5b9028270a00560d4d3136df1903d8a5ae24c9160946dd608dbcfb7309ec204feffffff1c3a4cfc973e05ccee521c597d3a0f49d84915e37e25e37eaba26ffeb6c52b66020000006b483045022100996e677306d57c7774f1d649000dbf03ce70becfaf9e8a780f83dd27cddd733002204e6f3a78315197c476ab61225d159b5d87ded73e3513e609bb7e1672f14b509c412102140b6dc0fc3620ab9e52c9058488f7a78823302915cf2718bba4e8e1f3e1be95feffffff040000000000000000396a04534c50000101044d494e5420e0f3e9ee5ab30c250c0d5fbd78c1eaedf3dfd998813ecbf29ee4c00dfb9acf120102080000000005f6113922020000000000001976a914a72678a69e85a64f3f371197285d5d8f93b6288888ac22020000000000001976a914a72678a69e85a64f3f371197285d5d8f93b6288888acff680200000000001976a91438b5e94bd38420ac5b24c6c5745c2eabeeb5543688aca74b0800"));
		com.bitcoin.indexer.blockchain.domain.Transaction minted = bitcoinJConverters.transaction(m, MainNetParams.get(), 0, false, "", Instant.ofEpochMilli(1000), Instant.ofEpochMilli(1000));

		SlpOpReturn slpOpReturn = minted.getSlpOpReturn().get(0);
		SlpValid valid = validatorFacade.isValid(minted.getTxId(), slpOpReturn.getTokenId().getHex(), slpOpReturn.getTokenType().getType(), minted.getOutputs(), minted.getInputs(), validatorFacade);

		assertThat(valid.getValid(), Matchers.is(SlpValid.Valid.VALID));
	}

	private String slpSendAfterGenesis() {

		return
				"01000000025e8bd5cb4fe7eddf7727c094656f57c7c08ffa3d68378c4287bb7d9075a59abe030000006a473044022002d5bb012bfa24c9f5ebddca5c36e8170848dd517291502c25b6b7c5df70c6ce022032bd0e7d663b85391145ae67be07a9d950f1ab413e03e2a36a458794fc3f956e41210331e03e4da02a0626ebc470dff23ba60922f5ce53045a55795c863249facd9f0cfeffffff5e8bd5cb4fe7eddf7727c094656f57c7c08ffa3d68378c4287bb7d9075a59abe010000006a47304402203abfd04b8f96ede7be4db07dc11f5120be0570dc8ea15129807842dd8600e31202200df503ca5ad39679c589528b92c2cf603df35ac1506344c2fe9b4d0cd7c8b7bf412103e133d71d9f793f8704486a1337e652965560eea6852e36394a5f4aa5409b0705feffffff040000000000000000406a04534c500001010453454e4420be9aa575907dbb87428c37683dfa8fc0c7576f6594c02777dfede74fcbd58b5e08000000000000000a08000000000001869622020000000000001976a91420f1f129ebe1fb357213e5069fe481e87622ef3388ac22020000000000001976a914d85d8816d3af825b83d2e4712463f52d72abf60388ac94ae0400000000001976a914118bcad9b52e2ff07538ada74298ddcb65f7893788ac761b0900";
	}

	//43f8d8861f597fefee7281d3303b7941c9387131c4d49b2685796a48bf62d3e4
	private String slpSendAfterSend() {

		return
				"010000000266ff688300877330cec5455cc1001c7e74a5c6ecebff9d00d3885e4f420f6f0e030000006a4730440220297080c4dba18c64174f6147800f7310014c311bda4b95c0ee0374d98185b2530220415fd4acaf4d74f1610c3d9cc77a398c85855ed974bc200ce6d4b6c4cdb12c0f41210207303c961fee966ea1081ae67f158e056103d054c14017a9b70f07c5cb62a6a3fefffffffa45ff1649731d5a5d1562d5c6a8ace52dea2fdfafa7138f5efcac4353f8982c020000006a4730440220711a51517b0aafc58a2540ebc001654c71d3fae168c80ac1a030ba5b6eea1217022056687b88dafd2683277b378d4943b798e076675817ba958b917732ef9090232b412102aa6709d83ab42f64b4880de2486756f88657cabe908e996e932265a312546077feffffff040000000000000000406a04534c500001010453454e4420be9aa575907dbb87428c37683dfa8fc0c7576f6594c02777dfede74fcbd58b5e0800000000000003e80800000000000182ae22020000000000001976a914ed5cc573dacc2fb6f78698881f62f40b945ae10c88ac22020000000000001976a9145d84c7f7f4e04b8e8b4bbeb3fc611af13247395988acd3800400000000001976a914ebec0839c8e5d89f44b5d6026089b779dfeee06c88acb51d0900";
	}

	private String slpGenesis() {
		return
				"01000000017cb991eaa4c8f8b26f1b693caa742b00fd18d983ffe2bbf196911ae9502b0013030000006a4730440220516d4ef15b02033a601fbd5b516917dc2d01438c598134f18296cbe0028194610220646804bd828337c92d9607770b0a77b63ab28925920649f4c3c76086933f59a84121037122c30aca041103e6678347417dd32feb9a50988d0669fbe7002c570e666545feffffff040000000000000000366a04534c500001010747454e4553495307414e544f4b454e0c416e6472656173546f6b656e4c004c00010001020800000000000186a022020000000000001976a91494fea682f3be6f675e0086a9470b5232202fd66588ac22020000000000001976a91494fea682f3be6f675e0086a9470b5232202fd66588ac97b20400000000001976a9142d299704a947a567ac45aea9bb2bf2d7f63ffc7788ac761b0900";
	}

	private String withBaton() {
		return
				"01000000017cb991eaa4c8f8b26f1b693caa742b00fd18d983ffe2bbf196911ae9502b0013030000006a4730440220516d4ef15b02033a601fbd5b516917dc2d01438c598134f18296cbe0028194610220646804bd828337c92d9607770b0a77b63ab28925920649f4c3c76086933f59a84121037122c30aca041103e6678347417dd32feb9a50988d0669fbe7002c570e666545feffffff040000000000000000366a04534c500001010747454e4553495307414e544f4b454e0c416e6472656173546f6b656e4c004c00010001020800000000000186a022020000000000001976a91494fea682f3be6f675e0086a9470b5232202fd66588ac22020000000000001976a91494fea682f3be6f675e0086a9470b5232202fd66588ac97b20400000000001976a9142d299704a947a567ac45aea9bb2bf2d7f63ffc7788ac761b0900";
	}

	private String mint() {
		return
				"0100000002fa45ff1649731d5a5d1562d5c6a8ace52dea2fdfafa7138f5efcac4353f8982c030000006b483045022100897833cd6d9acceeccb55d112c1b916f786bcc4d1dca78e2521ad8e1f22651ec022007c53e32c5b6f939d2261a093aced5ed23ddc973e41f39d7d508106ca00635e9412102cfc5e68f6e40b6888661d5518fa1195dfbc2dd25202eab11fa9be144b458ec91feffffff5e8bd5cb4fe7eddf7727c094656f57c7c08ffa3d68378c4287bb7d9075a59abe020000006a47304402206f613c0064e702391c64349697433a86d7714e8746e342eda6e640b4fb01190a02205fd752a0a30bad78fbc56616461eaccb8344b6b8c596ad2cfd144475f36ab829412103e133d71d9f793f8704486a1337e652965560eea6852e36394a5f4aa5409b0705feffffff040000000000000000396a04534c50000101044d494e5420be9aa575907dbb87428c37683dfa8fc0c7576f6594c02777dfede74fcbd58b5e01020800000000000007d022020000000000001976a9140cf38ffebd79223ccc6ff001df8f74d998e79c5188ac22020000000000001976a9140cf38ffebd79223ccc6ff001df8f74d998e79c5188ac98aa0400000000001976a914f72592b8621d4c286dc209ec6291baae8a49941e88ac761b0900";
	}

	private String previous() {
		return
				"01000000025e8bd5cb4fe7eddf7727c094656f57c7c08ffa3d68378c4287bb7d9075a59abe030000006a473044022002d5bb012bfa24c9f5ebddca5c36e8170848dd517291502c25b6b7c5df70c6ce022032bd0e7d663b85391145ae67be07a9d950f1ab413e03e2a36a458794fc3f956e41210331e03e4da02a0626ebc470dff23ba60922f5ce53045a55795c863249facd9f0cfeffffff5e8bd5cb4fe7eddf7727c094656f57c7c08ffa3d68378c4287bb7d9075a59abe010000006a47304402203abfd04b8f96ede7be4db07dc11f5120be0570dc8ea15129807842dd8600e31202200df503ca5ad39679c589528b92c2cf603df35ac1506344c2fe9b4d0cd7c8b7bf412103e133d71d9f793f8704486a1337e652965560eea6852e36394a5f4aa5409b0705feffffff040000000000000000406a04534c500001010453454e4420be9aa575907dbb87428c37683dfa8fc0c7576f6594c02777dfede74fcbd58b5e08000000000000000a08000000000001869622020000000000001976a91420f1f129ebe1fb357213e5069fe481e87622ef3388ac22020000000000001976a914d85d8816d3af825b83d2e4712463f52d72abf60388ac94ae0400000000001976a914118bcad9b52e2ff07538ada74298ddcb65f7893788ac761b0900";
	}

	//0e6f0f424f5e88d3009dffebecc6a5747e1c00c15c45c5ce307387008368ff66
	private String previousSend() {
		return
				"0100000002e30cd4948487bc47ee5459612f02b0f2ffda265e247ccaff986337d017772d61020000006b483045022100f2e4c9bbf212ce5b53dda476dae33acbb5c4049e3be7b989915e91d048bfec16022068240c4f6cdb678af6e54c32fcf287d15ab0678347f6300464200a69445d994a412102c196c909c91da0ffcca7b73351615dc4d6044b7e44c69aa16d5a048cdb2173bafeffffff9600ea4b714aaeff14337d93fd93616b4cd3ab662da22ec029d4129137e5ab190200000069463043021f329f1303a6d11585dafd127963d9fa1b1652a39d16ecf284938eb0d1582d34022018dc852f1c2d7fe33e8ebb6a51d5ec161d090eef58bba6b7663ace04e8ed7745412103763a62a526ab1411f29e01b716e98f416113dd30633566c8af0c29435f0ddc10feffffff040000000000000000406a04534c500001010453454e4420be9aa575907dbb87428c37683dfa8fc0c7576f6594c02777dfede74fcbd58b5e0800000000000003e808000000000000037a22020000000000001976a914ed5cc573dacc2fb6f78698881f62f40b945ae10c88ac22020000000000001976a914a2bedf881066b768fd3e3282b1a4019bc0c4ed4788acd6840400000000001976a914239b2abb9f78e01ec728c84e35545fdc741dfd0488acaa1d0900";
	}

	//612d7717d0376398ffca7c245e26dafff2b0022f615954ee47bc878494d40ce3
	private String tx612d7717d0376398ffca7c245e26dafff2b0022f615954ee47bc878494d40ce3() {
		return
				"0100000002c4bb42c01a7419ebb87694f89a9b6fdafcb79a6a2479d183f53590de1e108008030000006b483045022100ffd22a8e336774e88bc2ba3818a82bc6ddbda075556c64900057806286a0b36b02207a658899a551a7a9a4a7d305708daf1c0133f90663a5c81f6c73fd518baf0ca441210240b264528bf836bef5373b13df4da8b4d8d1ace96e37102ba9c01ecffa51b3affefffffff6fae300e35c83e8e78c7c081dda94a3bac1e4ad1877a30f02f7e6d8317481ad010000006b483045022100fe150375eeccdddfe6716d8b8612b78c4f00bd4fb86f4a74f07cece784bcd7ba022000ca918bdc48b9f6b370adb5dd37577a19b659ead7e6d3318563651f474c738941210330fe145ed7cae37ab7314fa50c12a98dd4a0915a6fc7fcedc7d5691c982ca1b5feffffff030000000000000000376a04534c500001010453454e442097d826999299f29cdeafa1352e2836f6ce223920a43221e57e3e06c879675afa08000000000000006422020000000000001976a9143c365e097639d884148a01c7601802f7cb9b8d5888acd9880400000000001976a9148fc1ed8b0af9989d83eb9a772ccabb44859807f788ac8d1b0900";
	}

	private String tx0880101ede9035f583d179246a9ab7fcda6f9b9af89476b8eb19741ac042bbc4() {
		return
				"0100000002f6fae300e35c83e8e78c7c081dda94a3bac1e4ad1877a30f02f7e6d8317481ad030000006a47304402201406f336c522c63eb6d3c0c925809bc31c2972345ad1b44860c1d9790c66fd06022053e02e735a3dc09777d804f0916d0920546644b0b4d90f044eb551758a6fe5ed4121039dcf715f1c0a8861b02ea953c0309db1b114b74bb92896c26e9f71587fdd0540fefffffff6fae300e35c83e8e78c7c081dda94a3bac1e4ad1877a30f02f7e6d8317481ad020000006a47304402200a08ad5a1771636d9d95c581c572f40c9ccbffbf0a6b6460fb17339caa88c3e8022056056be92df868ca8349d7098479fa388da2857384bbe8af4f38db6ac3c16789412102d6d1511b9ae6ca8079c86a4b5faeb907c140a63c8693368ea856a35e6e09c963feffffff040000000000000000406a04534c500001010453454e442097d826999299f29cdeafa1352e2836f6ce223920a43221e57e3e06c879675afa0800000000000003e808000000000001825222020000000000001976a9143c365e097639d884148a01c7601802f7cb9b8d5888ac22020000000000001976a914cb5d81efffd4b4b6ef6676e43735feb990a9b9b188ac8f8a0400000000001976a914725ca19a78ea61cee2256576dede2988f0fe0c1488ac8d1b0900";
	}

	private String txIdad817431d8e6f7020fa37718ade4c1baa394da1d087c8ce7e8835ce300e3faf6() {
		return
				"01000000029600ea4b714aaeff14337d93fd93616b4cd3ab662da22ec029d4129137e5ab19030000006a473044022042283ae96de58daaefbf08ada11cc18135a576cfbc3d13ef5cd79e55dbd836190220020d4c437bd5d77db0efb6312f2425541e1f7e5cb980a3542a4bbad091489caf4121034f5de781568c1b8be8cb217236d57f21db8bf063619193b7105e67800bfeed16feffffff7cb991eaa4c8f8b26f1b693caa742b00fd18d983ffe2bbf196911ae9502b0013020000006b483045022100aec02e4a6eae633ca81355a7dd983f4db6e784cc74e6707b82383aa45083754a022017cf7e94e666412d14a199e31bbdb9d2fb9c23428d80fb3a1d28ac2b4cdbd4684121030ab23eaa46c337932116d690d4663dc959f56e9e5f5bfaa2c349d38a6dfafee5feffffff040000000000000000406a04534c500001010453454e442097d826999299f29cdeafa1352e2836f6ce223920a43221e57e3e06c879675afa08000000000000006408000000000001863a22020000000000001976a9143c365e097639d884148a01c7601802f7cb9b8d5888ac22020000000000001976a914de6d2e86a65b635ced4c03587aaf746fdec88b7d88ac928e0400000000001976a9146e26c90e6100f546262d8727e8d6e7c9c4d40c3488ac8d1b0900";
	}
}