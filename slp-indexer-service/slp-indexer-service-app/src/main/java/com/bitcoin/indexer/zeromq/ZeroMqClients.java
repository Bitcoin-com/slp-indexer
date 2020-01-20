package com.bitcoin.indexer.zeromq;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.core.env.Environment;

import com.bitcoin.indexer.core.Coin;
import com.bitcoin.indexer.zeromq.ZeroMqClientImpl.ZeroMqSubscription;

public class ZeroMqClients {

	private final List<ZeroMqClientImpl> clients;
	private final ExecutorService clientThreads;

	public ZeroMqClients(Environment environment, ZeroMqSubscription subscription, Coin coin) {
		String peers = environment.getProperty("bitcoinj.peers." + coin.name().toLowerCase(), String.class, "");

		this.clients = Stream.of(peers.split(","))
				.map(s -> s.split(":"))
				.map(adr -> new ZeroMqClientImpl(adr[0], Integer.parseInt(adr[1]), subscription))
				.collect(Collectors.toList());
		if (!this.clients.isEmpty()) {
			this.clientThreads = Executors.newFixedThreadPool(this.clients.size());
		} else {
			// No hosts in config is effectively disabling the ZeroMQ feature
			this.clientThreads = null;
		}
	}

	public void start() {
		clients.forEach(clientThreads::execute);
	}

	public void destroy() {
		clients.forEach(ZeroMqClientImpl::destroy);
	}

}