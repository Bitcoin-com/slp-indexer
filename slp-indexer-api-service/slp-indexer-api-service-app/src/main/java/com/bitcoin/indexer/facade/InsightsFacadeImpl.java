package com.bitcoin.indexer.facade;

import java.util.Objects;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Request.Builder;
import okhttp3.Response;

public class InsightsFacadeImpl implements InsightsFacade {

	private OkHttpClient okHttpClient;
	private String insightsIP;
	private static final Logger logger = LoggerFactory.getLogger(InsightsFacadeImpl.class);

	public InsightsFacadeImpl(OkHttpClient okHttpClient, String insightsIP) {
		this.okHttpClient = Objects.requireNonNull(okHttpClient);
		this.insightsIP = Objects.requireNonNull(insightsIP);
	}

	@Override
	public Optional<InsightsResponse> getTxInfo(String txId) {
		Request request = new Builder()
				.url(insightsIP + "tx/" + txId)
				.get().build();
		try (Response res = okHttpClient.newCall(request).execute()) {
			if (res.isSuccessful()) {
				InsightsResponse value = new Gson().fromJson(res.body().string(), InsightsResponse.class);
				return Optional.ofNullable(value);
			}
		} catch (Exception e) {
			logger.error("Could not fetch insights data txId={}", txId);
		}
		return Optional.empty();
	}
}
