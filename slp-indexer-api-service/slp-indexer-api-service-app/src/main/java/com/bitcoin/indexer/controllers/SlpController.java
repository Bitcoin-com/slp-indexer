package com.bitcoin.indexer.controllers;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.bitcoin.indexer.blockchain.domain.Address;
import com.bitcoin.indexer.blockchain.domain.IndexerTransaction;
import com.bitcoin.indexer.blockchain.domain.Utxo;
import com.bitcoin.indexer.blockchain.domain.UtxoMinimalData;
import com.bitcoin.indexer.blockchain.domain.slp.ByteUtils;
import com.bitcoin.indexer.blockchain.domain.slp.ExtendedDetails;
import com.bitcoin.indexer.blockchain.domain.slp.SlpTokenDetails;
import com.bitcoin.indexer.blockchain.domain.slp.SlpTokenId;
import com.bitcoin.indexer.blockchain.domain.slp.SlpUtxo;
import com.bitcoin.indexer.blockchain.domain.slp.SlpValid;
import com.bitcoin.indexer.blockchain.domain.slp.SlpValid.Valid;
import com.bitcoin.indexer.core.Coin;
import com.bitcoin.indexer.facade.InsightsFacade;
import com.bitcoin.indexer.facade.InsightsResponse;
import com.bitcoin.indexer.metrics.SystemTimer;
import com.bitcoin.indexer.repository.SlpDetailsRepository;
import com.bitcoin.indexer.repository.TransactionRepository;
import com.bitcoin.indexer.repository.UtxoRepository;
import com.bitcoin.indexer.requests.AddressConverterRequest;
import com.bitcoin.indexer.requests.BalanceAddressTokenRequest;
import com.bitcoin.indexer.requests.BalanceForAddressRequest;
import com.bitcoin.indexer.requests.BalanceForTokenRequest;
import com.bitcoin.indexer.requests.BurnTotalRequest;
import com.bitcoin.indexer.requests.ExtendedDetailsRequest;
import com.bitcoin.indexer.requests.ValidateTxIdRequest;
import com.bitcoin.indexer.responses.AddressConvertResponse;
import com.bitcoin.indexer.responses.BalanceForTokenResponse;
import com.bitcoin.indexer.responses.BalanceResponse;
import com.bitcoin.indexer.responses.BurnCountResponse;
import com.bitcoin.indexer.responses.Detail;
import com.bitcoin.indexer.responses.Details;
import com.bitcoin.indexer.responses.ExtendedDetailsResponse;
import com.bitcoin.indexer.responses.Output;
import com.bitcoin.indexer.responses.RecentTransactionsResponse;
import com.bitcoin.indexer.responses.SlpValidateResponse;
import com.bitcoin.indexer.responses.TokenTransactionResponse;
import com.bitcoin.indexer.responses.TransactionTokenAddress;
import com.bitcoin.indexer.responses.TxDetailsResponse;
import com.bitcoin.indexer.responses.TxDetailsResponse.TokenInfo;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Scheduler;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;

// This controller should be burned along with rest.bitcoin.com/v2/slp api.......
@RequestMapping("/v2/slp")
@RestController
public class SlpController {

	private TransactionRepository transactionRepository;
	private UtxoRepository utxoRepository;
	private Coin coin;
	private SlpDetailsRepository detailsRepository;
	private InsightsFacade insightsFacade;
	private static final Logger logger = LoggerFactory.getLogger(SlpController.class);
	private static final int MAX_BATCH_SIZE = 3;
	private final Cache<String, ExtendedDetailsResponse> extendedDetailsResponseCache = Caffeine.newBuilder()
			.executor(Executors.newSingleThreadExecutor())
			.maximumSize(100)
			.expireAfterWrite(1, TimeUnit.DAYS)
			.build();
	private final Scheduler schedulers = Schedulers.from(Executors.newFixedThreadPool(50, new ThreadFactoryBuilder()
			.setNameFormat("refresh-cache-thread")
			.build()));

	private final ScheduledExecutorService refresh = Executors.newSingleThreadScheduledExecutor();

	private Set<String> bigTokens = Set.of("7f8889682d57369ed0e32336f8b7e0ffec625a35cca183f4e81fde4e71a538a1", //HONK
			"4de69e374a8ed21cbddd47f2338cc0f479dc58daa2bbe11cd604ca488eca0ddf", //SPICE
			"aa1cdd36ab9f4aa6284e5ff370421305887f845f076c38689bd912e372058c11", //TRIBE
			"6448381f9649ecacd8c30189cfbfee71a91b6b9738ea494fe33f8b8b51cbfca0"); //SOUR

	public SlpController(TransactionRepository transactionRepository,
			UtxoRepository utxoRepository,
			Coin coin,
			SlpDetailsRepository detailsRepository,
			InsightsFacade insightsFacade) {
		this.transactionRepository = transactionRepository;
		this.utxoRepository = utxoRepository;
		this.coin = coin;
		this.detailsRepository = detailsRepository;
		this.insightsFacade = Objects.requireNonNull(insightsFacade);

		refresh.scheduleWithFixedDelay(
				() -> {
					try {
						SystemTimer systemTimer = SystemTimer.create();
						systemTimer.start();
						makeExtendedListRequest(new ArrayList<>(bigTokens)).blockingGet();
						logger.info("Completed refresh of bigTokens time={}", systemTimer.getMsSinceStart());
					} catch (Exception e) {
						logger.error("Error refreshing", e);
					}
				}, 1000, 30000, TimeUnit.MILLISECONDS
		);

	}

	@GetMapping("/validateTxid/{txId}")
	public Single<SlpValidateResponse> slpValidateResponseSingle(@PathVariable String txId) {
		if (txId.length() != 64) {
			return Single.error(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Not a valid txId=" + txId));
		}

		return transactionRepository.fetchTransaction(txId, Coin.BCH, false)
				.map(e -> e.getTransaction().getSlpValid()
						.map(v -> new SlpValidateResponse(txId, getValid(v), v.getReason()))
						.orElse(new SlpValidateResponse(txId, false, "")))
				.toSingle(new SlpValidateResponse(txId, false, ""));
	}

	@PostMapping("/validateTxid")
	public Single<List<SlpValidateResponse>> slpValidateResponseSingle(@RequestBody ValidateTxIdRequest request) {
		if (request.txids.stream().anyMatch(txId -> txId.length() != 64)) {
			return Single.error(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Not a valid txId=" + request));
		}

		if (request.txids.size() > 500) {
			return Single.error(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Batch size cannot be larger than 500"));
		}

		return transactionRepository.fetchTransactions(request.txids, coin, true)
				.toFlowable()
				.flatMap(Flowable::fromIterable)
				.map(txs -> txs.getTransaction().getSlpValid()
						.map(v -> new SlpValidateResponse(txs.getTransaction().getTxId(), getValid(v), v.getReason()))
						.orElse(new SlpValidateResponse(txs.getTransaction().getTxId(), false, "")))
				.toList();
	}

	private boolean getValid(SlpValid slpValid) {
		if (slpValid.getValid() == Valid.VALID) {
			return true;
		}
		return false;
	}

	@GetMapping("/list")
	public Single<Object> deprecatedList() {
		return Single.error(() -> new ResponseStatusException(HttpStatus.GONE, "Endpoint is deprecated"));
	}

	@GetMapping("/list/{tokenId}")
	public Single<ExtendedDetailsResponse> details(@PathVariable String tokenId) {
		return extendedDetailsResponse(tokenId);
	}

	@PostMapping("/list")
	public Single<List<ExtendedDetailsResponse>> details(@RequestBody ExtendedDetailsRequest tokenDetailsRequest) {
		if (tokenDetailsRequest.tokenIds.size() > MAX_BATCH_SIZE) {
			return Single.error(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Batch size cannot be larger than " + MAX_BATCH_SIZE));
		}
		//To support legacy fields should be improved in V3
		return extendedDetails(tokenDetailsRequest);
	}

	@GetMapping("tokenStats/{tokenId}")
	public Single<ExtendedDetailsResponse> extendedDetailsResponse(@PathVariable String tokenId) {
		if (tokenId.length() != 64) {
			return Single.error(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Not a valid tokenId=" + tokenId));
		}

		ExtendedDetailsResponse cached = extendedDetailsResponseCache.getIfPresent(tokenId);
		if (cached != null) {
			refresh(tokenId, () -> makeExtendedRequest(tokenId).subscribeOn(schedulers).subscribe());
			return Single.just(cached);
		}
		return makeExtendedRequest(tokenId);
	}

	private Single<ExtendedDetailsResponse> makeExtendedRequest(@PathVariable String tokenId) {
		return Maybe.zip(detailsRepository.fetchExtendedDetails(List.of(new SlpTokenId(tokenId))).toMaybe(),
				transactionRepository.fetchTransaction(tokenId, coin, true),
				utxoRepository.fetchMinimalUtxoData(List.of(tokenId), false, Valid.VALID).toMaybe(),
				transactionRepository.transactionsForTokenId(tokenId).toMaybe(), (details, tx, utxos, numTxs) -> {
					Optional<ExtendedDetails> extendedDetails = details.stream().findFirst();

					boolean hasBaton = utxos.stream().anyMatch(UtxoMinimalData::isHasBaton);
					BigDecimal quantity = utxos.stream().map(UtxoMinimalData::getAmount)
							.reduce(BigDecimal::add).orElse(BigDecimal.ZERO);
					Integer activeMint = extendedDetails.flatMap(ExtendedDetails::getLastActiveMint).orElse(null);
					Integer lastActiveSend = extendedDetails.flatMap(ExtendedDetails::getLastActiveSend).orElse(null);
					ExtendedDetailsResponse extendedDetailsResponse = getExtendedDetailsResponse(extendedDetails.map(ExtendedDetails::getSlpTokenDetails).orElse(null),
							tx, utxos, hasBaton, quantity, activeMint, lastActiveSend,
							numTxs);

					Optional.ofNullable(extendedDetailsResponse).ifPresent(d -> {
						logger.info("Added to cache tokenId={}", tokenId);
						extendedDetailsResponseCache.put(tokenId, d);
					});
					return extendedDetailsResponse;
				})
				.doOnError(er -> logger.error("Error fetching extended details tokenId={}", tokenId, er))
				.onErrorResumeNext(Maybe.error(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR)))
				.toSingle(new ExtendedDetailsResponse());
	}

	@PostMapping("tokenStats")
	public Single<List<ExtendedDetailsResponse>> extendedDetails(@RequestBody ExtendedDetailsRequest request) {
		if (request.tokenIds.size() > MAX_BATCH_SIZE) {
			return Single.error(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Batch size cannot be larger than " + MAX_BATCH_SIZE));
		}

		List<String> tokens = request.tokenIds.stream().distinct().collect(Collectors.toList());
		List<ExtendedDetailsResponse> cached = new ArrayList<>();
		for (String token : new ArrayList<>(tokens)) {
			Optional<ExtendedDetailsResponse> response = refresh(token, () -> makeExtendedRequest(token).subscribeOn(schedulers).subscribe());
			response.ifPresent(r -> {
				cached.add(r);
				tokens.remove(token);
			});
		}
		if (tokens.isEmpty()) {
			return Single.just(cached);
		}

		return Single.zip(makeExtendedListRequest(tokens), Single.just(cached), (k, v) -> {
			List<ExtendedDetailsResponse> result = new ArrayList<>(k);
			result.addAll(v);
			return result;
		});
	}

	private Single<List<ExtendedDetailsResponse>> makeExtendedListRequest(List<String> tokens) {
		return Single.zip(detailsRepository.fetchExtendedDetails(tokens.stream().map(SlpTokenId::new).collect(Collectors.toList())),
				transactionRepository.fetchTransactions(tokens, coin, true),
				utxoRepository.fetchMinimalUtxoData(tokens, false, Valid.VALID),
				transactionRepository.transactionsForTokenIds(tokens), (details, txs, utxos, txCountForTokens) -> {
					Map<SlpTokenId, ExtendedDetails> idSlpTokenDetailsMap = details.stream().collect(Collectors.toMap(k -> k.getSlpTokenDetails().getTokenId(), v -> v));
					Map<String, IndexerTransaction> txIdTx = txs.stream().collect(Collectors.toMap(k -> k.getTransaction().getTxId(), v -> v));
					Map<String, List<UtxoMinimalData>> tokenIdUtxos = utxos.stream().collect(Collectors.groupingBy(UtxoMinimalData::getTokenId));
					return tokens.stream().map(t -> {
						ExtendedDetails extendedDetails = idSlpTokenDetailsMap.get(new SlpTokenId(t));
						SlpTokenDetails slpTokenDetails = extendedDetails.getSlpTokenDetails();
						IndexerTransaction genesis = txIdTx.get(t);
						List<UtxoMinimalData> txUtxos = tokenIdUtxos.containsKey(t) ? tokenIdUtxos.get(t) : List.of();
						Integer activeMint = extendedDetails.getLastActiveMint().orElse(null);
						Integer lastActiveSend = extendedDetails.getLastActiveSend().orElse(null);
						boolean hasBaton = hasBaton(txUtxos);
						BigDecimal quantity = getQuantity(txUtxos);
						ExtendedDetailsResponse extendedDetailsResponse = getExtendedDetailsResponse(slpTokenDetails, genesis, utxos, hasBaton, quantity, activeMint, lastActiveSend, txCountForTokens.get(t));
						Optional.ofNullable(extendedDetailsResponse).ifPresent(d -> {
							logger.trace("Added to cache tokenId={}", t);
							extendedDetailsResponseCache.put(t, d);
						});
						return extendedDetailsResponse;
					}).collect(Collectors.toList());
				})
				.filter(Objects::nonNull)
				.doOnError(er -> logger.error("Error fetching extended details tokenIds={}", String.join(":", tokens), er))
				.toSingle(List.of())
				.onErrorResumeNext(e -> Single.error(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error", e)));
	}

	@GetMapping("/convert/{address}")
	public Single<AddressConvertResponse> convertAddress(@PathVariable String address) {
		if (address.contains("simpleledger")) {
			Address legacy = Address.slpToBase58(address);
			Address cash = Address.base58ToCash(legacy.getAddress());
			return Single.just(new AddressConvertResponse(address, cash.getAddress(), legacy.getAddress()));
		}

		if (address.contains("bitcoincash")) {
			Address legacy = Address.cashAddressToBase58(address);
			Address slp = Address.base58ToSlp(legacy.getAddress());
			return Single.just(new AddressConvertResponse(slp.getAddress(), address, legacy.getAddress()));
		}

		Address slp = Address.base58ToSlp(address);
		Address cash = Address.base58ToCash(address);
		return Single.just(new AddressConvertResponse(slp.getAddress(), cash.getAddress(), address));
	}

	@PostMapping("/convert")
	public Single<List<AddressConvertResponse>> convertAddresses(@RequestBody AddressConverterRequest request) {
		if (request.addresses.size() > 10000) {
			return Single.error(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Batch size cannot be larger than " + 10000));
		}

		return Flowable.fromIterable(request.addresses)
				.flatMapSingle(this::convertAddress)
				.toList();
	}

	@GetMapping("balancesForAddress/{address}")
	public Single<List<BalanceResponse>> slpBalanceForAddress(@PathVariable String address) {
		Address base58 = getAddress(address);

		return utxoRepository.fetchSlpUtxosForAddress(base58, coin, false, Valid.VALID)
				.flatMap(utxos -> {
					Map<String, List<SlpUtxo>> utxosPerTokenId = utxos.stream().map(e -> e.getSlpUtxo().get())
							.collect(Collectors.groupingBy(k -> k.getSlpTokenId().getHex()));
					return Flowable.fromIterable(utxosPerTokenId.entrySet())
							.flatMapMaybe(entry -> detailsRepository.fetchSlpDetails(new SlpTokenId(entry.getKey()))
									.map(details -> {
										String tokenId = entry.getKey();
										BigDecimal totalTokenBalance = entry.getValue().stream().map(SlpUtxo::getAmount).reduce(BigDecimal::add).orElse(BigDecimal.ZERO);
										int decimals = details == null ? -1 : details.getDecimals();
										return new BalanceResponse(tokenId, totalTokenBalance, totalTokenBalance.toString(), address, decimals);
									})).toList();
				});
	}

	private Address getAddress(String address) {
		try {
			return Address.slpToBase58(address);
		} catch (Exception e) {
			try {
				return Address.cashAddressToBase58(address);
			} catch (Exception b) {
				return Address.create(address);
			}
		}
	}

	@PostMapping("balancesForAddress")
	public Single<List<List<BalanceResponse>>> slpBalanceForAddress(@RequestBody BalanceForAddressRequest request) {
		if (request.addresses.size() >= 20) {
			return Single.error(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Batch size cannot be larger than 20"));
		}
		List<Address> addresses = request.addresses.stream().map(this::getAddress).collect(Collectors.toList());

		return Flowable.fromIterable(addresses)
				.flatMapSingle(address -> utxoRepository.fetchSlpUtxosForAddress(address, coin, false, Valid.VALID))
				.flatMapSingle(utxos -> {
					Map<String, List<SlpUtxo>> utxosPerTokenId = utxos.stream().map(e -> e.getSlpUtxo().get())
							.collect(Collectors.groupingBy(k -> k.getSlpTokenId().getHex()));
					return Flowable.fromIterable(utxosPerTokenId.entrySet())
							.flatMapMaybe(entry -> detailsRepository.fetchSlpDetails(new SlpTokenId(entry.getKey()))
									.map(details -> {
										String tokenId = entry.getKey();
										String address = utxos.stream().findFirst().map(e -> e.getAddress().getAddress()).orElse(null);
										BigDecimal totalTokenBalance = entry.getValue().stream().map(SlpUtxo::getAmount).reduce(BigDecimal::add).orElse(BigDecimal.ZERO);
										int decimals = details == null ? -1 : details.getDecimals();
										return new BalanceResponse(tokenId, totalTokenBalance, totalTokenBalance.toString(), address, decimals);
									})).toList();
				})
				.toList()
				.doOnError(er -> logger.error("Could not fetch slp for addresses={}", request.addresses, er))
				.onErrorReturnItem(List.of());
	}

	@GetMapping("balancesForToken/{tokenId}")
	public Single<List<BalanceForTokenResponse>> balanceForToken(@PathVariable String tokenId) {
		if (tokenId.length() != 64) {
			return Single.error(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Not a valid tokenId=" + tokenId));
		}

		return utxoRepository.fetchUtxosWithTokenId(List.of(tokenId), false, Valid.VALID)
				.map(utxos -> {
					Map<Address, List<Utxo>> addressTokenUtxos = utxos.stream()
							.filter(e -> e.getSlpUtxo().isPresent())
							.collect(Collectors.groupingBy(Utxo::getAddress));
					return addressTokenUtxos.entrySet().stream()
							.map(entry -> {
								Address address = entry.getKey();
								BigDecimal totalTokenBalance = entry.getValue().stream()
										.map(e -> e.getSlpUtxo().get())
										.map(SlpUtxo::getAmount).reduce(BigDecimal::add).orElse(BigDecimal.ZERO);
								return new BalanceForTokenResponse(totalTokenBalance,
										totalTokenBalance.toPlainString(),
										Address.base58ToSlp(address.getAddress()).getAddress(), tokenId);
							}).collect(Collectors.toList());
				})
				.doOnError(er -> logger.error("Error fetching balanceFor tokenId={}", tokenId, er))
				.onErrorReturnItem(List.of());
	}

	@PostMapping("balancesForToken")
	public Single<List<List<BalanceForTokenResponse>>> balanceForToken(@RequestBody BalanceForTokenRequest request) {
		if (request.tokenIds.size() >= 2) {
			return Single.error(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Maximum 5 tokenIds are allowed=" + request.tokenIds.size()));
		}

		for (String tokenId : request.tokenIds) {
			if (tokenId.length() != 64) {
				return Single.error(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Not a valid tokenId=" + tokenId));
			}
		}

		return Flowable.fromIterable(request.tokenIds)
				.flatMapSingle(tokenId -> utxoRepository.fetchUtxosWithTokenId(List.of(tokenId), false, Valid.VALID)
						.map(utxos -> {
							Map<Address, List<Utxo>> addressTokenUtxos = utxos.stream()
									.filter(e -> e.getSlpUtxo().isPresent())
									.collect(Collectors.groupingBy(Utxo::getAddress));
							return addressTokenUtxos.entrySet().stream()
									.map(entry -> {
										Address address = entry.getKey();
										BigDecimal totalTokenBalance = entry.getValue().stream()
												.map(e -> e.getSlpUtxo().get())
												.map(SlpUtxo::getAmount).reduce(BigDecimal::add).orElse(BigDecimal.ZERO);
										return new BalanceForTokenResponse(totalTokenBalance,
												totalTokenBalance.toPlainString(),
												Address.base58ToSlp(address.getAddress()).getAddress(), tokenId);
									}).collect(Collectors.toList());
						})
						.doOnError(er -> logger.error("Error fetching balanceFor tokenId={}", tokenId, er))
						.onErrorReturnItem(List.of()))
				.toList();
	}

	@GetMapping("balance/{address}/{tokenId}")
	public Single<BalanceResponse> slpBalanceForAddressToken(@PathVariable String address, @PathVariable String tokenId) {
		Address base58 = getAddress(address);
		if (tokenId.length() != 64) {
			return Single.error(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Not a valid tokenId=" + tokenId));
		}

		return utxoRepository.fetchSlpUtxosForAddress(base58, coin, false, Valid.VALID)
				.zipWith(detailsRepository.fetchSlpDetails(new SlpTokenId(tokenId))
						.toSingle(new SlpTokenDetails(new SlpTokenId(tokenId), "", "", -1, "", null)), ((utxos, details) -> {
					Map<String, List<SlpUtxo>> utxosPerTokenId = utxos.stream().map(e -> e.getSlpUtxo().get())
							.collect(Collectors.groupingBy(k -> k.getSlpTokenId().getHex()));
					if (!utxosPerTokenId.containsKey(tokenId)) {
						return new BalanceResponse(tokenId, BigDecimal.ZERO, BigDecimal.ZERO.toString(), address, -1);
					}
					BigDecimal totalTokenBalance = utxosPerTokenId.get(tokenId).stream().map(SlpUtxo::getAmount).reduce(BigDecimal::add).orElse(BigDecimal.ZERO);
					return new BalanceResponse(tokenId, totalTokenBalance, totalTokenBalance.toString(), address, details.getDecimals());
				}));
	}

	@PostMapping("balance")
	public Single<List<BalanceResponse>> slpBalanceForAddressTokens(@RequestBody List<BalanceAddressTokenRequest> requests) {
		if (requests.size() > 100) {
			return Single.error(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Maximum 100 tokens are allowed=" + requests.size()));
		}

		for (BalanceAddressTokenRequest request : requests) {
			if (request.tokenId.length() != 64) {
				return Single.error(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Not a valid tokenId=" + request.tokenId));
			}
		}

		return Flowable.fromIterable(requests)
				.flatMapSingle(req -> utxoRepository.fetchSlpUtxosForAddress(getAddress(req.address), coin, false, Valid.VALID)
						.zipWith(detailsRepository.fetchSlpDetails(new SlpTokenId(req.tokenId)).timeout(10, TimeUnit.SECONDS)
								.toSingle(new SlpTokenDetails(new SlpTokenId(req.tokenId), "", "", -1, "", null)), (utxos, details) -> {
							Map<String, List<SlpUtxo>> utxosPerTokenId = utxos.stream().map(e -> e.getSlpUtxo().get())
									.collect(Collectors.groupingBy(k -> k.getSlpTokenId().getHex()));
							String tokenId = req.tokenId;
							String address = req.address;
							if (!utxosPerTokenId.containsKey(tokenId)) {
								return new BalanceResponse(tokenId, BigDecimal.ZERO, BigDecimal.ZERO.toString(), address, -1);
							}
							int decimals = details == null ? -1 : details.getDecimals();
							BigDecimal totalTokenBalance = utxosPerTokenId.get(tokenId).stream().map(SlpUtxo::getAmount).reduce(BigDecimal::add).orElse(BigDecimal.ZERO);
							return new BalanceResponse(tokenId, totalTokenBalance, totalTokenBalance.toString(), address, decimals);
						}))
				.toList();
	}

	@GetMapping("transactions/{tokenId}/{address}")
	public Single<List<TransactionTokenAddress>> transactionPerTokenAddress(@PathVariable String tokenId, @PathVariable String address) {
		SlpTokenDetails slpTokenDetails = detailsRepository.fetchSlpDetails(new SlpTokenId(tokenId)).timeout(10, TimeUnit.SECONDS).blockingGet();
		if (slpTokenDetails == null) {
			return Single.error(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token does not exist=" + tokenId));
		}

		return transactionRepository.fetchTransactions(getAddress(address), Coin.BCH)
				.toFlowable()
				.flatMap(Flowable::fromIterable)
				.filter(transaction -> {
					return transaction.getTransaction().getOutputs().stream()
							.filter(u -> u.getSlpUtxo().isPresent())
							.map(u -> u.getSlpUtxo().get())
							.anyMatch(u -> u.getSlpTokenId().getHex().equals(tokenId));
				})
				.map(filteredTx -> {
					boolean hasBaton = hasUtxoBaton(filteredTx.getTransaction().getOutputs());
					String transactionType = filteredTx.getTransaction().getOutputs().stream()
							.filter(u -> u.getSlpUtxo().isPresent())
							.findFirst()
							.map(u -> u.getSlpUtxo().get().getTokenTransactionType())
							.orElse("");

					Integer tokenType = filteredTx.getTransaction().getOutputs().stream()
							.filter(u -> u.getSlpUtxo().isPresent())
							.findFirst()
							.map(u -> u.getSlpUtxo().get().getTokenTypeHex())
							.map(Hex::decode)
							.map(ByteUtils.INSTANCE::toInt)
							.orElse(-1);

					List<Output> outputs = filteredTx.getTransaction().getOutputs().stream()
							.filter(e -> !e.getAddress().isOpReturn())
							.filter(e -> e.getSlpUtxo().isPresent())
							.map(utxo -> new Output(
									Address.base58ToSlp(utxo.getAddress().getAddress()).getAddress(),
									utxo.getSlpUtxo().map(SlpUtxo::getAmount)
											.map(BigDecimal::stripTrailingZeros)
											.orElse(utxo.getAmount()).toPlainString())
							)

							.collect(Collectors.toList());

					Detail detail = new Detail(slpTokenDetails.getDecimals(),
							tokenId,
							transactionType,
							tokenType,
							slpTokenDetails.getDocumentUri(),
							null,
							slpTokenDetails.getTicker(),
							slpTokenDetails.getName(),
							null,
							hasBaton,
							outputs);
					Details details = new Details(filteredTx.getTransaction().getSlpValid().orElse(SlpValid.unknown()).getValid() == Valid.VALID, detail);

					return new TransactionTokenAddress(
							filteredTx.getTransaction().getTxId(),
							details,
							null,
							73
					);
				})
				.toList()
				.doOnError(er -> logger.error("Could not fetch transactions for tokenId={} address={}", tokenId, address, er))
				.onErrorReturnItem(List.of());
	}

	@PostMapping("/transactions")
	public Single<List<List<TransactionTokenAddress>>> transactionsPerTokenAddress(@RequestBody List<BalanceAddressTokenRequest> requests) {
		if (requests.size() >= 100) {
			return Single.error(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Maximum 100 tokens are allowed=" + requests.size()));
		}

		for (BalanceAddressTokenRequest request : requests) {
			if (request.tokenId.length() != 64) {
				return Single.error(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Not a valid tokenId=" + request.tokenId));
			}
		}
		Map<String, SlpTokenDetails> detailsMap = new HashMap<>();
		for (BalanceAddressTokenRequest request : requests) {
			SlpTokenDetails slpTokenDetails = detailsRepository.fetchSlpDetails(new SlpTokenId(request.tokenId)).blockingGet();
			if (slpTokenDetails == null) {
				return Single.error(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token does not exist=" + request.tokenId));
			}
			detailsMap.put(request.tokenId, slpTokenDetails);
		}
		return Flowable.fromIterable(requests)
				.flatMapSingle(req -> transactionRepository.fetchTransactions(getAddress(req.address), Coin.BCH)
						.toFlowable()
						.flatMap(Flowable::fromIterable)
						.filter(transaction -> {
							return transaction.getTransaction().getOutputs().stream()
									.filter(u -> u.getSlpUtxo().isPresent())
									.map(u -> u.getSlpUtxo().get())
									.anyMatch(u -> u.getSlpTokenId().getHex().equals(req.tokenId));
						})
						.map(filteredTx -> {
							boolean hasBaton = hasUtxoBaton(filteredTx.getTransaction().getOutputs());
							SlpTokenDetails slpTokenDetails = detailsMap.get(req.tokenId);
							String transactionType = filteredTx.getTransaction().getOutputs().stream()
									.filter(u -> u.getSlpUtxo().isPresent())
									.findFirst()
									.map(u -> u.getSlpUtxo().get().getTokenTransactionType())
									.orElse("");

							Integer tokenType = filteredTx.getTransaction().getOutputs().stream()
									.filter(u -> u.getSlpUtxo().isPresent())
									.findFirst()
									.map(u -> u.getSlpUtxo().get().getTokenTypeHex())
									.map(Hex::decode)
									.map(ByteUtils.INSTANCE::toInt)
									.orElse(-1);

							Detail detail = new Detail(slpTokenDetails.getDecimals(),
									req.tokenId,
									transactionType,
									tokenType,
									slpTokenDetails.getDocumentUri(),
									null,
									slpTokenDetails.getTicker(),
									slpTokenDetails.getName(),
									null,
									hasBaton,
									filteredTx.getTransaction().getOutputs().stream()
											.filter(utxo -> !utxo.getAddress().isOpReturn())
											.filter(e -> e.getSlpUtxo().isPresent())
											.map(utxo -> {
												return new Output(
														Address.base58ToSlp(utxo.getAddress().getAddress()).getAddress(),
														utxo.getSlpUtxo().map(SlpUtxo::getAmount)
																.map(BigDecimal::stripTrailingZeros)
																.orElse(utxo.getAmount()).toPlainString());
											})
											.collect(Collectors.toList()));
							Details details = new Details(filteredTx.getTransaction().getSlpValid().orElse(SlpValid.unknown()).getValid() == Valid.VALID, detail);

							return new TransactionTokenAddress(
									filteredTx.getTransaction().getTxId(),
									details,
									null,
									73
							);
						})
						.toList()
						.onErrorReturnItem(List.of()))
				.toList();
	}

	@GetMapping("/burnTotal/{transactionId}")
	public Single<BurnCountResponse> burnCount(@PathVariable String transactionId) {
		if (transactionId.length() != 64) {
			return Single.error(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Not a valid transactionId=" + transactionId));
		}

		return transactionRepository.fetchTransaction(transactionId, coin, true)
				.map(tx -> {
					BigDecimal totaltTokenOut = tx.getTransaction().getOutputs()
							.stream()
							.filter(e -> e.getSlpUtxo().isPresent())
							.map(e -> e.getSlpUtxo().get())
							.map(SlpUtxo::getAmount).reduce(BigDecimal::add).orElse(BigDecimal.ZERO);

					BigDecimal inputTokenValue = tx.getTransaction().getInputs()
							.stream()
							.filter(e -> e.getSlpUtxo().isPresent())
							.map(e -> e.getSlpUtxo().get())
							.map(SlpUtxo::getAmount).reduce(BigDecimal::add).orElse(BigDecimal.ZERO);

					BigDecimal burnTotal = inputTokenValue.subtract(totaltTokenOut);

					return new BurnCountResponse(transactionId, inputTokenValue, totaltTokenOut, burnTotal);
				})
				.doOnError(er -> logger.error("Could not calculate burnTotal txId={}", transactionId, er))
				.onErrorReturnItem(new BurnCountResponse())
				.toSingle(new BurnCountResponse());
	}

	@PostMapping("burnTotal")
	public Single<List<BurnCountResponse>> burnTotal(@RequestBody BurnTotalRequest request) {
		if (request.txids.size() >= 3) {
			return Single.error(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Maximum 3 tokens are allowed=" + request.txids.size()));
		}

		for (String txId : request.txids) {
			if (txId.length() != 64) {
				return Single.error(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Not a valid transactionId=" + txId));
			}
		}
		return Flowable.fromIterable(request.txids)
				.flatMapSingle(this::burnCount)
				.toList();
	}

	@GetMapping("txDetails/{txId}")
	public Single<TxDetailsResponse> txDetails(@PathVariable String txId) {
		if (txId.length() != 64) {
			return Single.error(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Not a valid transactionId=" + txId));
		}

		Optional<InsightsResponse> txInfo = insightsFacade.getTxInfo(txId);
		if (txInfo.isEmpty()) {
			return Single.error(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not fetch transactionId=" + txId));
		}

		return transactionRepository.fetchTransaction(txId, Coin.BCH, true)
				.map(tx -> {
					List<SlpUtxo> slpUtxos = tx.getTransaction().getOutputs().stream()
							.filter(e -> e.getSlpUtxo().isPresent())
							.map(u -> u.getSlpUtxo().get())
							.collect(Collectors.toList());
					String hex = slpUtxos.get(0).getSlpTokenId().getHex();
					String tokenTransactionType = slpUtxos.get(0).getTokenTransactionType();
					List<String> outputs = slpUtxos.stream().map(SlpUtxo::getAmount).map(BigDecimal::toString).collect(Collectors.toList());
					TokenInfo tokenInfo = new TokenInfo(tokenTransactionType, 1, hex, outputs, tx.getTransaction().getSlpValid().orElse(SlpValid.unknown()).getValid() == Valid.VALID);
					return new TxDetailsResponse(txInfo.get(), tokenInfo);
				})
				.onErrorReturnItem(new TxDetailsResponse())
				.toSingle(new TxDetailsResponse());
	}

	@GetMapping("recentTxForTokenId/{tokenId}/{page}")
	public Single<RecentTransactionsResponse> recentTransactionsForTokenId(@PathVariable String tokenId, @PathVariable Integer page) {
		if (tokenId.length() != 64) {
			return Single.error(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Not a valid tokenId=" + tokenId));
		}

		int currentPage = page == null ? 0 : page;
		return transactionRepository.fetchTransactionsInvolvingToken(tokenId, true, currentPage)
				.map(list -> {
					List<TokenTransactionResponse> txs = list.stream().map(e -> TokenTransactionResponse.fromDomain(e.getTransaction())).collect(Collectors.toList());
					return new RecentTransactionsResponse(txs, currentPage);
				});
	}

	private List<IndexerTransaction> validTxs(List<IndexerTransaction> txs) {
		return txs.stream()
				.filter(t -> t.getTransaction().getSlpValid().map(SlpValid::getValid).orElse(Valid.UNKNOWN) == Valid.VALID)
				.collect(Collectors.toList());
	}

	private long getBlockCreated(Map<String, IndexerTransaction> txIdToTx, SlpTokenDetails d) {
		return txIdToTx.get(d.getTokenId().getHex()) != null ? txIdToTx.get(d.getTokenId().getHex()).getTransaction().getBlockHeight().orElse(-1) : -1;
	}

	private int getInitialTokenQty(Map<String, IndexerTransaction> txIdToTx, SlpTokenDetails d) {
		return txIdToTx.get(d.getTokenId().getHex()) != null ? getInitialTokenValue(txIdToTx.get(d.getTokenId().getHex())).intValue() : 0;
	}

	private BigDecimal getInitialTokenValue(IndexerTransaction transaction) {
		return transaction.getTransaction().getOutputs()
				.stream()
				.filter(e -> e.getSlpUtxo().isPresent())
				.map(e -> e.getSlpUtxo().get())
				.map(SlpUtxo::getAmount)
				.reduce(BigDecimal::add).orElse(BigDecimal.ZERO);
	}

	private ExtendedDetailsResponse getExtendedDetailsResponse(SlpTokenDetails details, IndexerTransaction tx, List<UtxoMinimalData> utxos, boolean hasBaton, BigDecimal quantity, Integer activeMint, Integer lastActiveSend, BigDecimal numTxs) {
		if (details == null || tx == null || utxos == null) {
			return null;
		}

		String hexString = tx.getTransaction().getOutputs().stream()
				.filter(e -> e.getSlpUtxo().isPresent())
				.map(e -> e.getSlpUtxo().get().getTokenTypeHex())
				.findFirst().orElse(null);

		BigDecimal initialTokenValue = getInitialTokenValue(tx);
		BigDecimal totalMinted = getTotalMinted(utxos);
		BigDecimal circulatingSupply = quantity;
		BigDecimal totalBurned = initialTokenValue.add(totalMinted).subtract(circulatingSupply).abs();
		boolean createdBaton = tx.getTransaction().getOutputs().stream().filter(e -> e.getSlpUtxo().isPresent())
				.anyMatch(u -> u.getSlpUtxo().get().hasBaton());

		return new ExtendedDetailsResponse(
				details.getDecimals(),
				details.getDocumentUri(),
				details.getTicker(),
				details.getName(),
				hasBaton,
				details.getTokenId().getHex(),
				initialTokenValue,
				tx.getTransaction().getBlockHeight().orElse(-1),
				quantity,
				lastActiveSend,
				activeMint,
				hexString,
				tx.getTransaction().getTime().toEpochMilli(),
				tx.getTransaction().getTime().toString(),
				totalMinted,
				totalBurned,
				circulatingSupply.toPlainString(),
				new BigDecimal(utxos.size()),
				numberValidAddresses(utxos),
				getLockedSatoshis(utxos),
				null,
				numTxs,
				getBatonStatus(hasBaton, createdBaton),
				lastActiveSend,
				activeMint
		);
	}

	private long numberValidAddresses(List<UtxoMinimalData> utxos) {
		return utxos.stream().map(e -> e.getAddress().getAddress())
				.collect(Collectors.toSet())
				.size();
	}

	private BigDecimal getQuantity(List<UtxoMinimalData> utxos) {
		return utxos.stream()
				.map(UtxoMinimalData::getAmount)
				.reduce(BigDecimal::add).orElse(BigDecimal.ZERO);
	}

	private String getBatonStatus(boolean hasBaton, boolean createdBaton) {
		if (hasBaton) {
			return "ALIVE";
		}
		if (createdBaton) {
			return "DEAD";
		}
		return "NEVER_CREATED";
	}

	private BigDecimal getLockedSatoshis(List<UtxoMinimalData> utxos) {
		return utxos.stream()
				.map(UtxoMinimalData::getAmount)
				.reduce(BigDecimal::add).orElse(BigDecimal.ZERO);
	}

	private BigDecimal getTotalMinted(List<UtxoMinimalData> utxos) {
		return utxos.stream()
				.filter(UtxoMinimalData::isMint)
				.map(UtxoMinimalData::getSatoshisValue)
				.reduce(BigDecimal::add).orElse(BigDecimal.ZERO);
	}

	private boolean hasBaton(List<UtxoMinimalData> utxos) {
		if (utxos == null) {
			return false;
		}

		return utxos.stream().anyMatch(UtxoMinimalData::isHasBaton);
	}

	private boolean hasUtxoBaton(List<Utxo> utxos) {
		if (utxos == null) {
			return false;
		}

		return utxos.stream().filter(e -> e.getSlpUtxo().isPresent())
				.anyMatch(u -> u.getSlpUtxo().get().hasBaton());
	}

	private Optional<ExtendedDetailsResponse> refresh(String tokenId, Runnable runnable) {
		ExtendedDetailsResponse ifPresent = extendedDetailsResponseCache.getIfPresent(tokenId);
		if (ifPresent != null) {
			if (!bigTokens.contains(tokenId)) {
				runnable.run();
			}
			return Optional.of(ifPresent);
		}
		return Optional.empty();
	}
}
