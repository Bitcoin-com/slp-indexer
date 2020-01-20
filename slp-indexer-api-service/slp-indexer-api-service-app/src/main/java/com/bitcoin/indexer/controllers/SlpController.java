package com.bitcoin.indexer.controllers;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import com.bitcoin.indexer.blockchain.domain.slp.SlpTokenDetails;
import com.bitcoin.indexer.blockchain.domain.slp.SlpTokenId;
import com.bitcoin.indexer.blockchain.domain.slp.SlpUtxo;
import com.bitcoin.indexer.blockchain.domain.slp.SlpValid;
import com.bitcoin.indexer.blockchain.domain.slp.SlpValid.Valid;
import com.bitcoin.indexer.core.Coin;
import com.bitcoin.indexer.facade.InsightsFacade;
import com.bitcoin.indexer.facade.InsightsResponse;
import com.bitcoin.indexer.repository.SlpDetailsRepository;
import com.bitcoin.indexer.repository.TransactionRepository;
import com.bitcoin.indexer.repository.UtxoRepository;
import com.bitcoin.indexer.requests.AddressConverterRequest;
import com.bitcoin.indexer.requests.BalanceAddressTokenRequest;
import com.bitcoin.indexer.requests.BalanceForAddressRequest;
import com.bitcoin.indexer.requests.BalanceForTokenRequest;
import com.bitcoin.indexer.requests.BurnTotalRequest;
import com.bitcoin.indexer.requests.ExtendedDetailsRequest;
import com.bitcoin.indexer.requests.TokenDetailsRequest;
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
import com.bitcoin.indexer.responses.TokenDetailsResponse;
import com.bitcoin.indexer.responses.TokenTransactionResponse;
import com.bitcoin.indexer.responses.TransactionTokenAddress;
import com.bitcoin.indexer.responses.TxDetailsResponse;
import com.bitcoin.indexer.responses.TxDetailsResponse.TokenInfo;

import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Single;

/*
This api is made to correspond to rest.bitcoin.com api.
New api endpoints should be done properly with versioning and separate controllers
*/

@RequestMapping("/v2/slp")
@RestController
public class SlpController {

	private TransactionRepository transactionRepository;
	private UtxoRepository utxoRepository;
	private Coin coin;
	private SlpDetailsRepository detailsRepository;
	private InsightsFacade insightsFacade;
	private static final Logger logger = LoggerFactory.getLogger(SlpController.class);

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

	@PostMapping("/validateTxid/")
	public Single<List<SlpValidateResponse>> slpValidateResponseSingle(@RequestBody ValidateTxIdRequest request) {
		if (request.txIds.stream().anyMatch(txId -> txId.length() != 64)) {
			return Single.error(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Not a valid txId=" + request));
		}

		if (request.txIds.size() >= 20) {
			return Single.error(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Batch size cannot be larger than 20"));
		}

		return transactionRepository.fetchTransactions(request.txIds, coin, true)
				.toFlowable()
				.flatMap(Flowable::fromIterable)
				.map(txs -> txs.getTransaction().getSlpValid()
						.map(v -> new SlpValidateResponse(txs.getTransaction().getTxId(), getValid(v), v.getReason()))
						.orElse(new SlpValidateResponse(txs.getTransaction().getTxId(), false, "")))
				.toList();
	}

	private boolean getValid(SlpValid slpValid) {
		return slpValid.getValid() != Valid.INVALID;
	}

	@GetMapping("/list")
	public Single<Object> deprecatedList() {
		return Single.error(() -> new ResponseStatusException(HttpStatus.GONE, "Endpoint is deprecated"));
	}

	@GetMapping("/list/{tokenId}")
	public Single<TokenDetailsResponse> details(@PathVariable String tokenId) {
		if (tokenId.length() != 64) {
			return Single.error(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Not a valid tokenId=" + tokenId));
		}

		return detailsRepository.fetchSlpDetails(new SlpTokenId(tokenId))
				.zipWith(transactionRepository.fetchTransaction(tokenId, coin, true), (details, tx) -> new TokenDetailsResponse(
						details.getDecimals(),
						details.getDocumentUri(),
						details.getTicker(),
						details.getName(),
						false,
						details.getTokenId().getHex(),
						getInitialTokenValue(tx).intValue(),
						tx.getTransaction().getBlockHeight().orElse(-1)
				))
				.doOnError(er -> logger.error("Could not fetch details for tokenId={}", tokenId, er))
				.onErrorReturnItem(new TokenDetailsResponse())
				.toSingle(new TokenDetailsResponse());
	}

	@PostMapping("/list/")
	public Single<List<TokenDetailsResponse>> details(@RequestBody TokenDetailsRequest tokenDetailsRequest) {
		if (tokenDetailsRequest.tokenIds.size() >= 5) {
			return Single.error(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Batch size cannot be larger than 20"));
		}

		List<SlpTokenId> ids = tokenDetailsRequest.tokenIds.stream().map(SlpTokenId::new).collect(Collectors.toList());
		return detailsRepository.fetchSlpDetails(ids)
				.zipWith(transactionRepository.fetchTransactions(tokenDetailsRequest.tokenIds, coin, true), (details, transactions) -> {
					Map<String, IndexerTransaction> txIdToTx = transactions.stream().collect(Collectors.toMap(e -> e.getTransaction().getTxId(), v -> v));
					return details.stream().map(d -> new TokenDetailsResponse(
							d.getDecimals(),
							d.getDocumentUri(),
							d.getTicker(),
							d.getName(),
							false,
							d.getTokenId().getHex(),
							getInitialTokenQty(txIdToTx, d),
							getBlockCreated(txIdToTx, d)))
							.collect(Collectors.toList());

				})
				.onErrorReturnItem(List.of());
	}

	@GetMapping("tokenStats/{tokenId}")
	public Single<ExtendedDetailsResponse> extendedDetailsResponse(@PathVariable String tokenId) {
		if (tokenId.length() != 64) {
			return Single.error(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Not a valid tokenId=" + tokenId));
		}

		return Maybe.zip(detailsRepository.fetchSlpDetails(new SlpTokenId(tokenId)),
				transactionRepository.fetchTransaction(tokenId, coin, true),
				utxoRepository.fetchUtxosWithTokenId(List.of(tokenId), false).toMaybe(), (details, tx, utxos) -> {
					boolean hasBaton = hasBaton(utxos);
					BigDecimal quantity = getQuantity(utxos);
					Integer activeMint = null;
					Integer lastActiveSend = null;
					return getExtendedDetailsResponse(details, tx, utxos, hasBaton, quantity, activeMint, lastActiveSend);
				})
				.doOnError(er -> logger.error("Error fetching extended details tokenId={}", tokenId, er))
				.onErrorReturnItem(new ExtendedDetailsResponse())
				.toSingle(new ExtendedDetailsResponse());
	}

	@PostMapping("tokenStats")
	public Single<List<ExtendedDetailsResponse>> extendedDetails(@RequestBody ExtendedDetailsRequest request) {
		if (request.tokenIds.size() >= 20) {
			return Single.error(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Batch size cannot be larger than 20"));
		}

		List<String> tokens = request.tokenIds.stream().distinct().collect(Collectors.toList());
		return Single.zip(detailsRepository.fetchSlpDetails(tokens.stream().map(SlpTokenId::new).collect(Collectors.toList())),
				transactionRepository.fetchTransactions(tokens, coin, true),
				utxoRepository.fetchUtxosWithTokenId(tokens, false), (details, tx, utxos) -> {
					Map<SlpTokenId, SlpTokenDetails> idSlpTokenDetailsMap = details.stream().collect(Collectors.toMap(SlpTokenDetails::getTokenId, v -> v));
					Map<String, IndexerTransaction> txIdTx = tx.stream().collect(Collectors.toMap(k -> k.getTransaction().getTxId(), v -> v));
					Map<String, List<Utxo>> txIdUtxos = utxos.stream().collect(Collectors.groupingBy(Utxo::getTxId));
					return tokens.stream().map(t -> {
						SlpTokenDetails slpTokenDetails = idSlpTokenDetailsMap.get(new SlpTokenId(t));
						IndexerTransaction indexerTransaction = txIdTx.get(t);
						List<Utxo> txUtxos = txIdUtxos.containsKey(t) ? txIdUtxos.get(t) : List.of();
						boolean hasBaton = hasBaton(txUtxos);
						BigDecimal quantity = getQuantity(txUtxos);
						return getExtendedDetailsResponse(slpTokenDetails, indexerTransaction, utxos, hasBaton, quantity, null, null);
					}).collect(Collectors.toList());
				})
				.filter(Objects::nonNull)
				.doOnError(er -> logger.error("Error fetching extended details tokenIds={}", String.join(":", request.tokenIds), er))
				.toSingle(List.of())
				.onErrorReturnItem(List.of());

	}

	@GetMapping("balancesForAddress/{address}")
	public Single<List<BalanceResponse>> slpBalanceForAddress(@PathVariable String address) {
		Address base58 = Address.slpToBase58(address);
		return utxoRepository.fetchSlpUtxosForAddress(base58, coin, false)
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

	@PostMapping("balancesForAddress")
	public Single<List<List<BalanceResponse>>> slpBalanceForAddress(@RequestBody BalanceForAddressRequest request) {
		if (request.addresses.size() >= 20) {
			return Single.error(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Batch size cannot be larger than 20"));
		}
		List<Address> addresses = request.addresses.stream().map(Address::slpToBase58).collect(Collectors.toList());

		return Flowable.fromIterable(addresses)
				.flatMapSingle(address -> utxoRepository.fetchSlpUtxosForAddress(address, coin, false))
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

		return utxoRepository.fetchUtxosWithTokenId(List.of(tokenId), false)
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
										totalTokenBalance.toString(),
										Address.base58ToSlp(address.getAddress()).getAddress(), tokenId);
							}).collect(Collectors.toList());
				})
				.doOnError(er -> logger.error("Error fetching balanceFor tokenId={}", tokenId, er))
				.onErrorReturnItem(List.of());
	}

	@PostMapping("balancesForToken")
	public Single<List<List<BalanceForTokenResponse>>> balanceForToken(@PathVariable BalanceForTokenRequest request) {
		if (request.tokenIds.size() >= 5) {
			return Single.error(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Maximum 5 tokenIds are allowed=" + request.tokenIds.size()));
		}

		for (String tokenId : request.tokenIds) {
			if (tokenId.length() != 64) {
				return Single.error(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Not a valid tokenId=" + tokenId));
			}
		}

		return Flowable.fromIterable(request.tokenIds)
				.flatMapSingle(tokenId -> utxoRepository.fetchUtxosWithTokenId(List.of(tokenId), false)
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
												totalTokenBalance.toString(),
												Address.base58ToSlp(address.getAddress()).getAddress(), tokenId);
									}).collect(Collectors.toList());
						})
						.doOnError(er -> logger.error("Error fetching balanceFor tokenId={}", tokenId, er))
						.onErrorReturnItem(List.of()))
				.toList();
	}

	@GetMapping("balance/{address}/{tokenId}")
	public Single<BalanceResponse> slpBalanceForAddressToken(@PathVariable String address, @PathVariable String tokenId) {
		Address base58 = Address.slpToBase58(address);
		if (tokenId.length() != 64) {
			return Single.error(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Not a valid tokenId=" + tokenId));
		}

		return utxoRepository.fetchSlpUtxosForAddress(base58, coin, false)
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
		if (requests.size() >= 5) {
			return Single.error(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Maximum 5 tokens are allowed=" + requests.size()));
		}

		for (BalanceAddressTokenRequest request : requests) {
			if (request.tokenId.length() != 64) {
				return Single.error(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Not a valid tokenId=" + request.tokenId));
			}
		}

		return Flowable.fromIterable(requests)
				.flatMapSingle(req -> utxoRepository.fetchSlpUtxosForAddress(Address.slpToBase58(req.address), coin, false)
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

		return transactionRepository.fetchTransactions(Address.slpToBase58(address), Coin.BCH)
				.toFlowable()
				.flatMap(Flowable::fromIterable)
				.filter(transaction -> {
					return transaction.getTransaction().getOutputs().stream()
							.filter(u -> u.getSlpUtxo().isPresent())
							.map(u -> u.getSlpUtxo().get())
							.anyMatch(u -> u.getSlpTokenId().getHex().equals(tokenId));
				})
				.map(filteredTx -> {
					boolean hasBaton = hasBaton(filteredTx.getTransaction().getOutputs());
					String transactionType = filteredTx.getTransaction().getOutputs().stream()
							.filter(u -> u.getSlpUtxo().isPresent())
							.findFirst()
							.map(u -> u.getSlpUtxo().get().getTokenTransactionType())
							.orElse("");

					Detail detail = new Detail(slpTokenDetails.getDecimals(),
							tokenId,
							transactionType,
							null,
							slpTokenDetails.getDocumentUri(),
							null,
							slpTokenDetails.getTicker(),
							slpTokenDetails.getName(),
							null,
							hasBaton,
							filteredTx.getTransaction().getOutputs().stream()
									.map(utxo -> new Output(utxo.getAddress().getAddress(), utxo.getAmount().toString())).collect(Collectors.toList()));
					Details details = new Details(filteredTx.getTransaction().getSlpValid().orElse(SlpValid.unknown()).getValid() == Valid.VALID, detail);

					return new TransactionTokenAddress(
							filteredTx.getTransaction().getTxId(),
							details,
							"",
							73
					);
				})
				.toList()
				.doOnError(er -> logger.error("Could not fetch transactions for tokenId={} address={}", tokenId, address, er))
				.onErrorReturnItem(List.of());
	}

	@PostMapping("/transactions")
	public Single<List<List<TransactionTokenAddress>>> transactionsPerTokenAddress(@RequestBody List<BalanceAddressTokenRequest> requests) {
		if (requests.size() >= 3) {
			return Single.error(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Maximum 3 tokens are allowed=" + requests.size()));
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
				.flatMapSingle(req -> transactionRepository.fetchTransactions(Address.slpToBase58(req.address), Coin.BCH)
						.toFlowable()
						.flatMap(Flowable::fromIterable)
						.filter(transaction -> transaction.getTransaction().getOutputs().stream()
								.filter(u -> u.getSlpUtxo().isPresent())
								.map(u -> u.getSlpUtxo().get())
								.anyMatch(u -> u.getSlpTokenId().getHex().equals(req.tokenId)))
						.map(filteredTx -> {
							boolean hasBaton = hasBaton(filteredTx.getTransaction().getOutputs());
							SlpTokenDetails slpTokenDetails = detailsMap.get(req.tokenId);
							String transactionType = filteredTx.getTransaction().getOutputs().stream()
									.filter(u -> u.getSlpUtxo().isPresent())
									.findFirst()
									.map(u -> u.getSlpUtxo().get().getTokenTransactionType())
									.orElse("");

							Detail detail = new Detail(slpTokenDetails.getDecimals(),
									req.tokenId,
									transactionType,
									null,
									slpTokenDetails.getDocumentUri(),
									null,
									slpTokenDetails.getTicker(),
									slpTokenDetails.getName(),
									null,
									hasBaton,
									filteredTx.getTransaction().getOutputs().stream()
											.map(utxo -> new Output(utxo.getAddress().getAddress(), utxo.getAmount().toString())).collect(Collectors.toList()));
							Details details = new Details(filteredTx.getTransaction().getSlpValid().orElse(SlpValid.unknown()).getValid() == Valid.VALID, detail);

							return new TransactionTokenAddress(
									filteredTx.getTransaction().getTxId(),
									details,
									"",
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
				.onErrorReturnItem(new BurnCountResponse())
				.toSingle(new BurnCountResponse());
	}

	@PostMapping("burnTotal")
	public Single<List<BurnCountResponse>> burnTotal(@RequestBody BurnTotalRequest request) {
		if (request.txIds.size() >= 3) {
			return Single.error(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Maximum 3 tokens are allowed=" + request.txIds.size()));
		}

		for (String txId : request.txIds) {
			if (txId.length() != 64) {
				return Single.error(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Not a valid transactionId=" + txId));
			}
		}
		return Flowable.fromIterable(request.txIds)
				.flatMapSingle(this::burnCount)
				.toList();
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

	@PostMapping("/convert/")
	public Single<List<AddressConvertResponse>> convertAddresses(@RequestBody AddressConverterRequest request) {
		return Flowable.fromIterable(request.addresses)
				.flatMapSingle(this::convertAddress)
				.toList();
	}

	@GetMapping("txDetails/{txId}")
	public Single<TxDetailsResponse> txDetails(@PathVariable String txId) {
		if (txId.length() != 64) {
			return Single.error(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Not a valid transactionId=" + txId));
		}

		Optional<InsightsResponse> txInfo = insightsFacade.getTxInfo(txId);
		if (txInfo.isEmpty()) {
			return Single.error(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Not a valid transactionId=" + txId));
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

	private ExtendedDetailsResponse getExtendedDetailsResponse(SlpTokenDetails details, IndexerTransaction tx, List<Utxo> utxos, boolean hasBaton, BigDecimal quantity, Integer activeMint, Integer lastActiveSend) {
		if (details == null || tx == null || utxos == null) {
			return null;
		}
		return new ExtendedDetailsResponse(
				details.getDecimals(),
				details.getDocumentUri(),
				details.getTicker(),
				details.getName(),
				hasBaton,
				details.getTokenId().getHex(),
				getInitialTokenValue(tx),
				tx.getTransaction().getBlockHeight().orElse(-1),
				quantity,
				new BigDecimal(utxos.size()),
				numberValidAddresses(utxos),
				getLockedSatoshis(utxos),
				lastActiveSend,
				activeMint);
	}

	private int numberValidAddresses(List<Utxo> utxos) {
		return utxos.stream().map(e -> e.getAddress().getAddress())
				.collect(Collectors.toSet())
				.size();
	}

	private BigDecimal getQuantity(List<Utxo> utxos) {
		return utxos.stream()
				.filter(e -> e.getSlpUtxo().isPresent())
				.map(e -> e.getSlpUtxo().get())
				.map(SlpUtxo::getAmount)
				.reduce(BigDecimal::add).orElse(BigDecimal.ZERO);
	}

	private BigDecimal getLockedSatoshis(List<Utxo> utxos) {
		return utxos.stream()
				.map(Utxo::getAmount)
				.reduce(BigDecimal::add).orElse(BigDecimal.ZERO);

	}

	private boolean hasBaton(List<Utxo> utxos) {
		if (utxos == null) {
			return false;
		}

		return utxos.stream().filter(e -> e.getSlpUtxo().isPresent())
				.anyMatch(u -> u.getSlpUtxo().get().hasBaton());
	}
}
