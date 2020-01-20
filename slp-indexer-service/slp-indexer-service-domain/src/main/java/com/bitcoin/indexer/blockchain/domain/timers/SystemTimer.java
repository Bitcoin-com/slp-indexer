package com.bitcoin.indexer.blockchain.domain.timers;

public class SystemTimer {

	private long start;

	public static SystemTimer create() {
		return new SystemTimer();
	}

	public void start() {
		start = System.currentTimeMillis();
	}

	public long getMsSinceStart() {
		return System.currentTimeMillis() - start;
	}

}