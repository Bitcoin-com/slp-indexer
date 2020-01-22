package com.bitcoin.indexer.facade;

import static org.hamcrest.MatcherAssert.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.script.ScriptBuilder;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mockito;
import org.spongycastle.util.encoders.Hex;
import org.springframework.data.util.Pair;

import com.bitcoin.indexer.FakeBlockRepo;
import com.bitcoin.indexer.FakeInMemoryDetails;
import com.bitcoin.indexer.FakeSlpValidator;
import com.bitcoin.indexer.FakeUtxoRepository;
import com.bitcoin.indexer.blockchain.domain.Address;
import com.bitcoin.indexer.blockchain.domain.IndexerTransaction;
import com.bitcoin.indexer.blockchain.domain.Input;
import com.bitcoin.indexer.blockchain.domain.Utxo;
import com.bitcoin.indexer.blockchain.domain.slp.SlpOpReturn;
import com.bitcoin.indexer.blockchain.domain.slp.SlpValid;
import com.bitcoin.indexer.core.Coin;
import com.bitcoin.indexer.facade.validators.GenesisValidatorAssumeParentValid;
import com.bitcoin.indexer.facade.validators.MintValidatorAssumeParentValid;
import com.bitcoin.indexer.facade.validators.SendValidatorAssumeParentValid;
import com.bitcoin.indexer.facade.validators.SlpValidatorCustomImplAssumeParentValid;
import com.bitcoin.indexer.facade.validators.SlpValidatorFacade;
import com.bitcoin.indexer.repository.TransactionRepository;
import com.bitcoin.indexer.repository.UtxoRepository;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import io.micrometer.core.instrument.util.IOUtils;
import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Single;

@RunWith(Parameterized.class)
public class SLPDBTestVectorTest {

	private final List<Pair<String, Boolean>> whenResult;
	private final String shouldHex;
	private final boolean shouldValid;
	private final String description;
	private SlpValidatorFacade slpValidatorFacade;
	private Map<String, IndexerTransaction> inMemory = new HashMap<>();
	private BitcoinJConverters bitcoinJConverters;
	private BitcoinJConverters changeAbleValidation;
	private FakeSlpValidator preValidationFacade = new FakeSlpValidator(true);
	private UtxoRepository utxoRepository;

	//This test will be ugly because of the non-mapping models of slpdb and this indexer
	@Test()
	public void parameterized_validation() {
		System.out.println(description);
		for (Pair<String, Boolean> stringBooleanPair : whenResult) {
			Transaction spy = Mockito.spy(new Transaction(MainNetParams.get(), Hex.decode(stringBooleanPair.getFirst())));
			String hashAsString = spy.getHashAsString();
			Mockito.when(spy.getHashAsString()).thenReturn(hashAsString);
			spy.addInput(Sha256Hash.wrap("4c6ce5d34e7a3287c8931b4c6e023ed629332a712e02722578797a6f8fe874e5"), 0, ScriptBuilder.createOutputScript(org.bitcoinj.core.Address.fromBase58(MainNetParams.get(), "1CmGtV9qxGK8Jy23R74Y4zm8ko7MJ7wa4E")));
			preValidationFacade.setValid(stringBooleanPair.getSecond());
			com.bitcoin.indexer.blockchain.domain.Transaction alwaysValidParent = changeAbleValidation
					.transaction(spy, MainNetParams.get(), 0, false, null, Instant.now(), null);
			if (alwaysValidParent.isSlp()) {
				SlpOpReturn slpOpReturn = alwaysValidParent.getSlpOpReturn().get(0);
				IndexerTransaction value = IndexerTransaction.create(alwaysValidParent)
						.withValid(preValidationFacade.isValid(hashAsString, slpOpReturn.getTokenId().getHex(), slpOpReturn.getTokenType().getType(), alwaysValidParent.getOutputs(), alwaysValidParent.getInputs(), slpValidatorFacade));
				inMemory.put(alwaysValidParent.getTxId(), value);
				utxoRepository.saveUtxo(value.getTransaction().getOutputs(), Coin.BCH);
			}
		}

		Transaction transaction = new Transaction(MainNetParams.get(), Hex.decode(shouldHex));
		com.bitcoin.indexer.blockchain.domain.Transaction tx = bitcoinJConverters.transaction(transaction, MainNetParams.get(), 0, false, null, Instant.now(), null);
		List<Input> withValue = tx.getInputs().stream()
				.map(e -> {
					Utxo utxo = utxoRepository.fetchUtxo(e.getTxId(), e.getIndex(), Coin.BCH).blockingGet();
					if (utxo != null) {
						return Input.knownValue(
								utxo.getAddress(),
								utxo.getAmount(),
								utxo.getIndex(),
								utxo.getTxId(),
								utxo.getSlpUtxo().orElse(null),
								false,
								0
						);
					}
					return e;
				}).collect(Collectors.toList());

		if (!tx.getSlpOpReturn().isEmpty()) {
			SlpValid valid = slpValidatorFacade.isValid(tx.getTxId(), tx.getSlpOpReturn().get(0).getTokenId().getHex(), tx.getSlpOpReturn().get(0).getTokenType().getType(), tx.getOutputs(), withValue, slpValidatorFacade);
			tx = IndexerTransaction.create(tx).withValid(valid).getTransaction();
		}

		if (shouldValid) {
			assertThat(description, tx.getSlpValid().get().getValid(), Matchers.is(SlpValid.Valid.VALID));
		} else {
			assertThat(description, tx.getSlpValid().get().getValid(), Matchers.is(SlpValid.Valid.INVALID));
		}
	}

	@Parameters(name = "{index}: {3}")
	public static Collection<Object[]> data() {
		JsonParser jsonParser = new JsonParser();
		List<Object[]> list = new ArrayList<>();

		for (JsonElement jsonElement : jsonParser.parse(testCases).getAsJsonArray()) {
			String description = jsonElement.getAsJsonObject().get("description").getAsString();
			List<Pair<String, Boolean>> whenResult = new ArrayList<>();
			for (JsonElement when : jsonElement.getAsJsonObject().get("when").getAsJsonArray()) {
				String whenHex = when.getAsJsonObject().get("tx").getAsString();
				boolean whenValid = when.getAsJsonObject().get("valid").getAsBoolean();
				whenResult.add(Pair.of(whenHex, whenValid));
			}

			JsonArray should = jsonElement.getAsJsonObject().get("should").getAsJsonArray();
			String shouldHex = should.get(0).getAsJsonObject().get("tx").getAsString();
			boolean shouldValid = should.get(0).getAsJsonObject().get("valid").getAsBoolean();
			list.add(new Object[] { whenResult, shouldHex, shouldValid, description });
		}
		return list;
	}

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
		utxoRepository = new FakeUtxoRepository();
		slpValidatorFacade = new SlpValidatorCustomImplAssumeParentValid(transactionRepository,
				new MintValidatorAssumeParentValid(transactionRepository, utxoRepository),
				new SendValidatorAssumeParentValid(transactionRepository, utxoRepository),
				new GenesisValidatorAssumeParentValid(transactionRepository, utxoRepository));
		bitcoinJConverters = new BitcoinJConverters(new FakeInMemoryDetails(), new FakeUtxoRepository(), Coin.BCH, new FakeBlockRepo());
		changeAbleValidation = new BitcoinJConverters(new FakeInMemoryDetails(), new FakeUtxoRepository(), Coin.BCH, new FakeBlockRepo());
	}

	public static String testCases;

	static {
		testCases = IOUtils.toString(SLPDBTestVectorTest.class.getClassLoader().getResourceAsStream("slpdb_test.json"), StandardCharsets.UTF_8);
	}

	public SLPDBTestVectorTest(List<Pair<String, Boolean>> whenResult, String shouldHex, boolean shouldValid, String description) {
		this.whenResult = whenResult;
		this.shouldHex = shouldHex;
		this.shouldValid = shouldValid;
		this.description = description;
	}

}
