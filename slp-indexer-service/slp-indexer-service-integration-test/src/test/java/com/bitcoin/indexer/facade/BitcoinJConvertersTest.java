package com.bitcoin.indexer.facade;

import static org.junit.Assert.assertThat;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bitcoinj.core.BitcoinSerializer;
import org.bitcoinj.core.Block;
import org.bitcoinj.core.Context;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.params.MainNetParams;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.spongycastle.util.encoders.Hex;
import org.springframework.data.util.Pair;

import com.bitcoin.indexer.blockchain.domain.Address;
import com.bitcoin.indexer.blockchain.domain.Input;
import com.bitcoin.indexer.blockchain.domain.Utxo;
import com.bitcoin.indexer.blockchain.domain.slp.SlpOpReturnGenesis;
import com.bitcoin.indexer.blockchain.domain.slp.SlpOpReturnSend;
import com.bitcoin.indexer.blockchain.domain.slp.SlpTokenDetails;
import com.bitcoin.indexer.blockchain.domain.slp.SlpTokenId;
import com.bitcoin.indexer.blockchain.domain.slp.SlpUtxo;
import com.bitcoin.indexer.blockchain.domain.slp.SlpVerifiedToken;
import com.bitcoin.indexer.config.SystemTimer;
import com.bitcoin.indexer.core.Coin;
import com.bitcoin.indexer.repository.BlockRepository;
import com.bitcoin.indexer.repository.SlpDetailsRepository;
import com.bitcoin.indexer.repository.UtxoRepository;

import io.micrometer.core.instrument.util.IOUtils;
import io.reactivex.Maybe;
import io.reactivex.Single;

public class BitcoinJConvertersTest {

	private Map<String, SlpTokenDetails> tokenDetailsMap = new HashMap<>();

	private BitcoinJConverters bitcoinJConverters = new BitcoinJConverters(new SlpDetailsRepository() {
		@Override
		public Maybe<SlpTokenDetails> fetchSlpDetails(SlpTokenId slpTokenId) {
			return Maybe.just(new SlpTokenDetails(slpTokenId, "", "", 0, "", SlpVerifiedToken.create(slpTokenId, false, "", "")));
		}

		@Override
		public Single<List<SlpTokenDetails>> fetchSlpDetails(List<SlpTokenId> slpTokenIds) {
			return Single.just(List.of());
		}

		@Override
		public Single<SlpTokenDetails> saveSlpTokenDetails(SlpTokenDetails slpTokenDetails) {
			tokenDetailsMap.put(slpTokenDetails.getTokenId().getHex(), slpTokenDetails);
			return Single.just(slpTokenDetails);
		}

	}, new UtxoRepository() {
		@Override
		public Single<List<Utxo>> fetchUtxosFromAddress(Address address, Coin coin, boolean useCache) {
			return null;
		}

		@Override
		public Single<List<Utxo>> fetchSlpUtxosForAddress(Address address, Coin coin, boolean useCache) {
			return null;
		}

		@Override
		public Maybe<Utxo> fetchUtxo(String txId, int inputIndex, Coin coin) {
			return Maybe.empty();
		}

		@Override
		public Single<List<Utxo>> fetchUtxosWithTokenId(List<String> tokenIds, boolean isSpent) {
			return Single.just(List.of());
		}

		@Override
		public Single<List<Utxo>> fetchUtxos(List<Pair<String, Integer>> txIdIndexs, Coin coin) {
			return null;
		}

		@Override
		public Single<List<Utxo>> fetchUtxo(List<Input> inputs, Coin coin) {
			return Single.just(List.of());
		}

		@Override
		public Single<List<Utxo>> fetchUtxos(String txId, Coin coin) {
			return Single.just(List.of());
		}

		@Override
		public Single<List<Utxo>> saveUtxo(List<Utxo> utxos, Coin coin) {
			return Single.just(List.of());
		}

		@Override
		public Single<List<Utxo>> spendUtxo(List<Utxo> utxos, Coin coin) {
			return Single.just(List.of());
		}

		@Override
		public Single<List<Utxo>> removeUtxo(List<Utxo> utxos, Coin coin) {
			return Single.just(List.of());
		}

		@Override
		public void addToCache(Utxo utxo) {

		}
	}, Coin.BCH, new BlockRepository() {
		@Override
		public Single<com.bitcoin.indexer.blockchain.domain.Block> saveBlock(com.bitcoin.indexer.blockchain.domain.Block block) {
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
		public Maybe<com.bitcoin.indexer.blockchain.domain.Block> getBlock(String hash) {
			return Maybe.empty();
		}
	});

	@Before
	public void setup() {

	}

	@Test
	public void parses_slp_genesis_correctly() {
		Transaction transaction;
		transaction = new Transaction(MainNetParams.get(), Hex.decode(
				"01000000017cb991eaa4c8f8b26f1b693caa742b00fd18d983ffe2bbf196911ae9502b0013030000006a4730440220516d4ef15b02033a601fbd5b516917dc2d01438c598134f18296cbe0028194610220646804bd828337c92d9607770b0a77b63ab28925920649f4c3c76086933f59a84121037122c30aca041103e6678347417dd32feb9a50988d0669fbe7002c570e666545feffffff040000000000000000366a04534c500001010747454e4553495307414e544f4b454e0c416e6472656173546f6b656e4c004c00010001020800000000000186a022020000000000001976a91494fea682f3be6f675e0086a9470b5232202fd66588ac22020000000000001976a91494fea682f3be6f675e0086a9470b5232202fd66588ac97b20400000000001976a9142d299704a947a567ac45aea9bb2bf2d7f63ffc7788ac761b0900"));

		com.bitcoin.indexer.blockchain.domain.Transaction convertedTx = bitcoinJConverters.transaction(transaction, MainNetParams.get(), 0, false, "", Instant.ofEpochMilli(200), Instant.ofEpochMilli(200));

		assertThat(convertedTx.getSlpOpReturn(), Matchers.not(Matchers.empty()));

		SlpOpReturnGenesis genesis = (SlpOpReturnGenesis) convertedTx.getSlpOpReturn().get(0);

		assertThat(genesis.getTicker(), Matchers.is("ANTOKEN"));

		SlpUtxo slpUtxo = convertedTx.getOutputs().get(1).getSlpUtxo().get();
		assertThat(slpUtxo.getAmount(), Matchers.is(new BigDecimal("100000")));
		assertThat(slpUtxo.hasBaton(), Matchers.is(false));
	}

	@Test
	public void parses_slp_send_correctly() {
		Transaction transaction = new Transaction(MainNetParams.get(), Hex.decode(
				"01000000025e8bd5cb4fe7eddf7727c094656f57c7c08ffa3d68378c4287bb7d9075a59abe030000006a473044022002d5bb012bfa24c9f5ebddca5c36e8170848dd517291502c25b6b7c5df70c6ce022032bd0e7d663b85391145ae67be07a9d950f1ab413e03e2a36a458794fc3f956e41210331e03e4da02a0626ebc470dff23ba60922f5ce53045a55795c863249facd9f0cfeffffff5e8bd5cb4fe7eddf7727c094656f57c7c08ffa3d68378c4287bb7d9075a59abe010000006a47304402203abfd04b8f96ede7be4db07dc11f5120be0570dc8ea15129807842dd8600e31202200df503ca5ad39679c589528b92c2cf603df35ac1506344c2fe9b4d0cd7c8b7bf412103e133d71d9f793f8704486a1337e652965560eea6852e36394a5f4aa5409b0705feffffff040000000000000000406a04534c500001010453454e4420be9aa575907dbb87428c37683dfa8fc0c7576f6594c02777dfede74fcbd58b5e08000000000000000a08000000000001869622020000000000001976a91420f1f129ebe1fb357213e5069fe481e87622ef3388ac22020000000000001976a914d85d8816d3af825b83d2e4712463f52d72abf60388ac94ae0400000000001976a914118bcad9b52e2ff07538ada74298ddcb65f7893788ac761b0900"));

		com.bitcoin.indexer.blockchain.domain.Transaction convertedTx = bitcoinJConverters.transaction(transaction, MainNetParams.get(), 0, false, "", Instant.MAX, Instant.ofEpochMilli(200));

		assertThat(convertedTx.getSlpOpReturn(), Matchers.not(Matchers.empty()));

		SlpOpReturnSend send = (SlpOpReturnSend) convertedTx.getSlpOpReturn().get(0);

		assertThat(send.getQuantities().get(0), Matchers.is(new BigInteger("10")));

		SlpUtxo slpUtxo = convertedTx.getOutputs().get(1).getSlpUtxo().get();

		assertThat(convertedTx.getOutputs().get(1).getAmount(), Matchers.is(new BigDecimal(transaction.getOutputs().get(1).getValue().toString())));

		assertThat(slpUtxo.getAmount(), Matchers.is(new BigDecimal("10")));

	}

	@Test
	public void parses_slp_mint_correctly() {
		Transaction transaction = new Transaction(MainNetParams.get(), Hex.decode(
				"0100000002fa45ff1649731d5a5d1562d5c6a8ace52dea2fdfafa7138f5efcac4353f8982c030000006b483045022100897833cd6d9acceeccb55d112c1b916f786bcc4d1dca78e2521ad8e1f22651ec022007c53e32c5b6f939d2261a093aced5ed23ddc973e41f39d7d508106ca00635e9412102cfc5e68f6e40b6888661d5518fa1195dfbc2dd25202eab11fa9be144b458ec91feffffff5e8bd5cb4fe7eddf7727c094656f57c7c08ffa3d68378c4287bb7d9075a59abe020000006a47304402206f613c0064e702391c64349697433a86d7714e8746e342eda6e640b4fb01190a02205fd752a0a30bad78fbc56616461eaccb8344b6b8c596ad2cfd144475f36ab829412103e133d71d9f793f8704486a1337e652965560eea6852e36394a5f4aa5409b0705feffffff040000000000000000396a04534c50000101044d494e5420be9aa575907dbb87428c37683dfa8fc0c7576f6594c02777dfede74fcbd58b5e01020800000000000007d022020000000000001976a9140cf38ffebd79223ccc6ff001df8f74d998e79c5188ac22020000000000001976a9140cf38ffebd79223ccc6ff001df8f74d998e79c5188ac98aa0400000000001976a914f72592b8621d4c286dc209ec6291baae8a49941e88ac761b0900"));

		com.bitcoin.indexer.blockchain.domain.Transaction convertedTx = bitcoinJConverters.transaction(transaction, MainNetParams.get(), 0, false, "", Instant.MAX, Instant.ofEpochMilli(200));

		assertThat(convertedTx.getSlpOpReturn(), Matchers.not(Matchers.empty()));

		SlpUtxo slpUtxo = convertedTx.getOutputs().get(1).getSlpUtxo().get();

		assertThat(slpUtxo.getAmount(), Matchers.is(new BigDecimal("2000")));
		assertThat(slpUtxo.hasBaton(), Matchers.is(false));

		slpUtxo = convertedTx.getOutputs().get(2).getSlpUtxo().get();
		assertThat(slpUtxo.getAmount(), Matchers.is(new BigDecimal("0")));
		assertThat(slpUtxo.hasBaton(), Matchers.is(true));
	}

	@Test
	public void parses_normal_tx_correctly() {
		Transaction transaction = new Transaction(MainNetParams.get(), Hex.decode(
				"0100000002bf52995c05b9d6d665d2a141addc92f08e849fd5168567e7d53c00e6305cc6fbd30000006a473044022046d476b40132bc6eaeffda7e86e4aa753b9e160be2d71722d4e0db31794b17bf022015dd691b916b3a5e9ae3b117ed1591bb6bc453de4570b093713fd7ba305f0b69412103390e121888d94987dd77d74a40e643eaa24ae61410533046b363d602096312ceffffffff2e4bdcef816c779aeab9cd51494eb2f95ddafac6deadc7b2c19185bb7ca41b44010000006a473044022008700e7ac6146e09e42d2b43aa27b381d81ed2cb1b78f7e66eca77a594f6c4c402205dbe660516b6c17e8bea7e28575ac42493f1be94b1c9e404a80ebf970303174c4121036b7b02cc5592256d22e45c2c70c41b34e962cc370bcf672cea6f476e1db318c8ffffffff03e4550106000000001976a91408a8c8ec954a7bb45d05e2002ea0f1b49a67fae788acc00c1e55010000001976a91455fc049b4ec950a10129e634635283c0c364d6af88acae05ca00000000001976a91464aed5047c3bd7deddd2e4c75e0df1a2efb14a1788ac00000000"));
		com.bitcoin.indexer.blockchain.domain.Transaction convertedTx = bitcoinJConverters.transaction(transaction, MainNetParams.get(), 0, false, "", Instant.MAX, Instant.ofEpochMilli(200));
		assertThat(convertedTx.getOutputs(), Matchers.hasSize(3));
	}

	//9740e7d646f5278603c04706a366716e5e87212c57395e0d24761c0ae784b2c6
	@Test
	public void parses_weird_out_address_outputs_correctly() {
		Transaction transaction = new Transaction(MainNetParams.get(), Hex.decode(
				"010000000121eb234bbd61b7c3d31034762126a64ff046b074963bf359eaa0da0ab59203a0010000008b4830450220263325fcbd579f5a3d0c49aa96538d9562ee41dc690d50dcc5a0af4ba2b9efcf022100fd8d53c6be9b3f68c74eed559cca314e718df437b5c5c57668c5930e1414050201410452eca3b9b42d8fac888f4e6a962197a386a8e1c423e852dfbc58466a8021110ec5f1588cec8b4ebfc4be8a4d920812a39303727a90d53e82a70adcd3f3d15f09ffffffff01a0860100000000006b4c684c554b452d4a522049532041205045444f5048494c4521204f682c20616e6420676f642069736e2774207265616c2c207375636b612e2053746f7020706f6c6c7574696e672074686520626c6f636b636861696e207769746820796f7572206e6f6e73656e73652eac00000000"));
		com.bitcoin.indexer.blockchain.domain.Transaction convertedTx = bitcoinJConverters.transaction(transaction, MainNetParams.get(), 0, false, "", Instant.MAX, Instant.ofEpochMilli(200));
		assertThat(convertedTx.getOutputs(), Matchers.hasSize(1));
	}

	//000000000000079e848a95192f6fbf629555e518704f6441f049477ec55e545b
	@Test
	public void parse_block() throws Exception {
		String rawBlock = IOUtils.toString(BitcoinJConvertersTest.class.getClassLoader().getResourceAsStream("rawblock.txt"), StandardCharsets.UTF_8);
		Context orCreate = Context.getOrCreate(MainNetParams.get());
		Block block = new BitcoinSerializer(MainNetParams.get(), false).makeBlock(Hex.decode(rawBlock));

		SystemTimer systemTimer = SystemTimer.create();
		systemTimer.start();
		bitcoinJConverters.block(block, 142623, MainNetParams.get());
		System.out.println(systemTimer.getMsSinceStart());
	}

	//ba91704e0c0a2a56cce95f89c39f53699075b35daa2d94758d07b4b298b4c852
	@Test
	public void parses_slp_genesis() {
		Transaction transaction = new Transaction(MainNetParams.get(), Hex.decode(
				"01000000021560763b8ee79392698cc747db5432a16c49e471409524c5172a23d82a9ef8c2010000006a47304402205c494575d4eac81ad94eada74e44e3a8ffc8b7df59602749c1e3c7481b0628f402204216a4d52bffc4e7b15c30ae3daae971eacfabd2defb738018b25813a3e06a5841210257c282b2e7e49573e37dc62fed0759bb78bee80a852cce7b6bd77679a7c01378feffffff045ced2872e4ab5781be1f609929503a87a880c68f0613ad833c1c6715861dea000000006a473044022005064f6476ad876b447df39f226b7f5bd05000e1227d18b34dfc0e243079f11e022047cc74b43b0fd2e35ac8cf01e42b65be8ea2848cb7454ef0a2e416f2e6ae8f184121025bc9fab941f48482a7d988ae22810dc664deb32b8587ed4051e7cc0e7bf78ab8feffffff020000000000000000976a04534c500001010747454e45534953045445535404544553544c4d626974636f696e66696c65733a62383662346263626162376364373837623163383933636131303132353063386334363764626261346466323239623131383231386264386139653835613932208eacdc879a3c7c398bde3a6381de812115355694e6752a110217d7ed17a6b15c010901020800038d7ea4c6800122020000000000001976a91451359d7e536bf880211311a0823f50343ce64ffc88ac09600800"));
		com.bitcoin.indexer.blockchain.domain.Transaction convertedTx = bitcoinJConverters.transaction(transaction, MainNetParams.get(), 0, false, "", Instant.MAX, Instant.ofEpochMilli(200));

		assertThat(transaction.getOutputs().get(1).getValue().longValue(), Matchers.is(convertedTx.getOutputs().get(1).getAmount().longValue()));
		assertThat(convertedTx.getOutputs(), Matchers.hasSize(transaction.getOutputs().size()));
	}

	//d02ec96b900a8f76d98d16cb672d4bb4a81bc39a2b2a7385f9f8583a78ef986d
	@Test
	public void parses_correctly() {
		Transaction transaction = new Transaction(MainNetParams.get(), Hex.decode(
				"020000000285c4f4ba84dab3137a7dc83d8c7e80ef6e45a710e4967684115393ebae89de71000000006b483045022100877bc2fd1984887aeab3d78d7ebb9a823bfddc453a7e4fb123572644b25b8ae0022030b2fb1a16e8af240e099b97f901e405d21766783b5c6c976601a1e542d7a56241210263d20290b06957948f168f43d19b347b4d877616ea544346622ab69322868014ffffffff7235bf0ebfe1a72e31cbeb483ffd8f1109894567dcb9f48b85119041f821c0c2010000006b483045022100edd6db10242879a5b0c7d4b4f21a7b469aa4e0f17e4bb5b97253c0ed88eaa755022024e7fdc124669709763a04289059112a0a241db545e29ef6ce96a1b6cfbcc5c041210263d20290b06957948f168f43d19b347b4d877616ea544346622ab69322868014ffffffff020000000000000000406a04534c500001010453454e4420dd84ca78db4d617221b58eabc6667af8fe2f7eadbfcc213d35be9f1b419beb8d08000000000000000008000000000000000122020000000000001976a91498dbc61c5fd996f0b5e8357f47e44f70d798136388ac00000000"));
		com.bitcoin.indexer.blockchain.domain.Transaction convertedTx = bitcoinJConverters.transaction(transaction, MainNetParams.get(), 0, false, "", Instant.MAX, Instant.ofEpochMilli(200));

		assertThat(transaction.getOutputs().get(1).getValue().longValue(), Matchers.is(convertedTx.getOutputs().get(1).getAmount().longValue()));
		assertThat(convertedTx.getOutputs(), Matchers.hasSize(transaction.getOutputs().size()));
	}

	//bc179baab547b7d7c1d5d8d6f8b0cc6318eaa4b0dd0a093ad6ac7f5a1cb6b3ba
	@Test
	public void parses_address() {
		Transaction transaction = new Transaction(MainNetParams.get(), Hex.decode(
				"010000000290c5e425bfba62bd5b294af0414d8fa3ed580c5ca6f351ccc23e360b14ff7f470100000091004730440220739d9ab2c3e7089e7bd311f267a65dc0ea00f49619cb61ec016a5038016ed71202201b88257809b623d471e429787c36e0a9bcd2a058fc0c75fd9c25f905657e3b9e01ab512103c86390eb5230237f31de1f02e70ce61e77f6dbfefa7d0e4ed4f6b3f78f85d8ec2103193f28067b502b34cac9eae39f74dba4815e1278bab31516efb29bd8de2c1bea52aeffffffffdd7f3ce640a2fb04dbe24630aa06e4299fbb1d3fe585fe4f80be4a96b5ff0a0d01000000b400483045022100a28d2ace2f1cb4b2a58d26a5f1a2cc15cdd4cf1c65cee8e4521971c7dc60021c0220476a5ad62bfa7c18f9174d9e5e29bc0062df543e2c336ae2c77507e462bbf95701ab512103c86390eb5230237f31de1f02e70ce61e77f6dbfefa7d0e4ed4f6b3f78f85d8ec2103193f28067b502b34cac9eae39f74dba4815e1278bab31516efb29bd8de2c1bea21032462c60ebc21f4d38b3c4ccb33be77b57ae72762be12887252db18fd6225befb53aeffffffff02e0fd1c00000000001976a9148501106ab5492387998252403d70857acfa1586488ac50c3000000000000171499050637f553f03cc0f82bbfe98dc99f10526311b17500000000"));
		com.bitcoin.indexer.blockchain.domain.Transaction convertedTx = bitcoinJConverters.transaction(transaction, MainNetParams.get(), 0, false, "", Instant.MAX, Instant.ofEpochMilli(200));
		assertThat(convertedTx.getInputs(), Matchers.hasSize(2));
	}

	//60a20bd93aa49ab4b28d514ec10b06e1829ce6818ec06cd3aabd013ebcdc4bb1
	@Test
	public void parses_correct_outputs() {
		Transaction transaction = new Transaction(MainNetParams.get(), Hex.decode(
				"010000000337bd40a022eea1edd40a678cddabe200b131afd5797b232ac21861d8e97eb367020000008a4730440220e8343f8ac7e96582d92a450ce314668db4f7a0e2c94a97aa6df026f93ebee2290220866b5728d4247688d91b4a30144762bc8bfd7f385de7f7d326d665ff5e3e900301410461cbdcc5409fb4b4d42b51d33381354d80e550078cb532a34bfa2fcfdeb7d76519aecc62770f5b0e4ef8551946d8a540911abe3e7854a26f39f58b25c15342afffffffff96420befb14a9357181e5da089824a3e6ea5a95856ff74c06c7d5ea98d633cf9020000008a4730440220b7227a8f816f3810f97057102edf8be4434c1e00f48b4440976bcc478f1431030220af3cba150afdd44618de4369cdc65fea73e447d7b5fbe135d2f08f86d82aa85f01410461cbdcc5409fb4b4d42b51d33381354d80e550078cb532a34bfa2fcfdeb7d76519aecc62770f5b0e4ef8551946d8a540911abe3e7854a26f39f58b25c15342afffffffff96420befb14a9357181e5da089824a3e6ea5a95856ff74c06c7d5ea98d633cf9010000008a47304402207d689e1a61e06440eab18d961517a97c49219a91f2c59d9630e902fcb2f4ea8b0220dcd274349ca264d8bd2bee5135664a92899e94a319a349d6d6e3660d04b564ad0141047a4c5d104002ebc203bef5cab6f13ff57ab624bb5f9f1186beb64c83a396da0d912e11a18ea15a2c784a62fed2bbd8258c3413c18bf4c3f2ba28f3d5565e328bffffffff0340420f000000000087514104cc71eb30d653c0c3163990c47b976f3fb3f37cccdcbedb169a1dfef58bbfbfaff7d8a473e7e2e6d317b87bafe8bde97e3cf8f065dec022b51d11fcdd0d348ac4410461cbdcc5409fb4b4d42b51d33381354d80e550078cb532a34bfa2fcfdeb7d76519aecc62770f5b0e4ef8551946d8a540911abe3e7854a26f39f58b25c15342af52ae50cec402000000001976a914c812a297b8e0e778d7a22bb2cd6d23c3e789472b88ac20a10700000000001976a914641ad5051edd97029a003fe9efb29359fcee409d88ac00000000"));
		com.bitcoin.indexer.blockchain.domain.Transaction convertedTx = bitcoinJConverters.transaction(transaction, MainNetParams.get(), 0, false, "", Instant.MAX, Instant.ofEpochMilli(200));
		assertThat(convertedTx.getOutputs(), Matchers.hasSize(3));
	}

	//54756dc575ecfb5798028108c775a4163720bd3cc894355ba7c16b323dbdef84
	@Test
	public void parsing_minting_correctly() {
		Transaction transaction = new Transaction(MainNetParams.get(), Hex.decode(
				"0100000002fa45ff1649731d5a5d1562d5c6a8ace52dea2fdfafa7138f5efcac4353f8982c030000006b483045022100897833cd6d9acceeccb55d112c1b916f786bcc4d1dca78e2521ad8e1f22651ec022007c53e32c5b6f939d2261a093aced5ed23ddc973e41f39d7d508106ca00635e9412102cfc5e68f6e40b6888661d5518fa1195dfbc2dd25202eab11fa9be144b458ec91feffffff5e8bd5cb4fe7eddf7727c094656f57c7c08ffa3d68378c4287bb7d9075a59abe020000006a47304402206f613c0064e702391c64349697433a86d7714e8746e342eda6e640b4fb01190a02205fd752a0a30bad78fbc56616461eaccb8344b6b8c596ad2cfd144475f36ab829412103e133d71d9f793f8704486a1337e652965560eea6852e36394a5f4aa5409b0705feffffff040000000000000000396a04534c50000101044d494e5420be9aa575907dbb87428c37683dfa8fc0c7576f6594c02777dfede74fcbd58b5e01020800000000000007d022020000000000001976a9140cf38ffebd79223ccc6ff001df8f74d998e79c5188ac22020000000000001976a9140cf38ffebd79223ccc6ff001df8f74d998e79c5188ac98aa0400000000001976a914f72592b8621d4c286dc209ec6291baae8a49941e88ac761b0900"));
		com.bitcoin.indexer.blockchain.domain.Transaction convertedTx = bitcoinJConverters.transaction(transaction, MainNetParams.get(), 0, false, "", Instant.MAX, Instant.ofEpochMilli(200));

		Utxo utxo = convertedTx.getOutputs().get(1);
		assertThat(utxo.getSlpUtxo().get().getAmount(), Matchers.is(new BigDecimal("2000")));

	}

	//0053f979b04858665cbec9d077059f406ae359bffb961e867677e2ec5df9cff2
	@Test
	public void parses_HUGE_tx_correctly() {
		Transaction transaction = new Transaction(MainNetParams.get(), Hex.decode(
				"0100000001d529033631c7fa14fafade8426052b8bfdd01fa6ed2d3f22ade7dfa9232d5109030000006b4830450221009103a29c125d1058423648de6ea9465872cbd3c68b6f2238b2568d697603c507022072555695c1f1c94c0255eb51d0510dc32ec40a01de04b63ea76fa79e46298ee64121033e16ecc44c980407f48b3cb2f34d0fa33aff4b67c9061f43c10e0b93732eb2bcfeffffff040000000000000000286a04534c500001010747454e455349534c0004485547454c004c000100010208f9ccd8a1c508000022020000000000001976a914b529c0e0b336b322a98a155a5737d7c36db5a43f88ac22020000000000001976a914b529c0e0b336b322a98a155a5737d7c36db5a43f88acfcbd0000000000001976a9143f87296771b367e2ea5b4dee7dbe2967ff6f031f88ac8d4d0800"));
		com.bitcoin.indexer.blockchain.domain.Transaction convertedTx = bitcoinJConverters.transaction(transaction, MainNetParams.get(), 0, false, "", Instant.MAX, Instant.ofEpochMilli(200));

		Utxo utxo = convertedTx.getOutputs().get(1);
		assertThat(utxo.getSlpUtxo().get().getAmount(), Matchers.is(new BigDecimal("18000000000000000000")));

	}
	/*//
	@Test
	public void invalid() {
		Transaction transaction = new Transaction(MainNetParams.get(), Hex.decode(
				"01000000021aa9e7e1a6b89dea5a2632bf0e562913034ac94524b6d594f2ea673872c6fa21010000006b4830450221009ea1e7062d0a7c425dff774ad915cd75c8780bbd13da0de0bdde163a2738611302202fbff9c04b66b03d7dc05fec4a88982022b37071ed6a08e3da7268c5ee91348441210298f83f4a9f74ee10e51d911f072321f5616031725b68a762934c8ee3034ec9efffffffff8ed014006e82d05b957cb7628aa7d92afe20347b2b00184654b74cb68c551a1a000000006a473044022008cf6a044ee383bf01946409ccabb75b1caab3664bc2d17b2d2187dad5854f460220679479f33070b453a60f4c22488ede86b32e59a4d451c451a2157fb88485659341210298f83f4a9f74ee10e51d911f072321f5616031725b68a762934c8ee3034ec9efffffffff030000000000000000376a04534c500001010453454e44204de69e374a8ed21cbddd47f2338cc0f479dc58daa2bbe11cd604ca488eca0ddf08000000000000038422020000000000001976a914516443bc72e55487d18cd7659b5ea5e9727524d088ac22020000000000001976a914516443bc72e55487d18cd7659b5ea5e9727524d088ac00000000"));
		com.bitcoin.indexer.blockchain.domain.Transaction convertedTx = bitcoinJConverters.transaction(transaction, MainNetParams.get(), 0, false, "", Instant.MAX, Instant.ofEpochMilli(200));

		assertThat(convertedTx.getSlpValid().get(), Matchers.is(SlpValid.INVALID));
	}*/
}
