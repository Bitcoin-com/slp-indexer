package com.bitcoin.indexer.config;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.micrometer.core.instrument.Timer;
import io.reactivex.Maybe;
import io.reactivex.Single;

public class MethodTimer {

	private static final Logger logger = LoggerFactory.getLogger(MethodTimer.class);

	public static <T> Single<T> timed(Single<T> reactor, Timer metric) {
		SystemTimer timer = SystemTimer.create();
		return reactor
				.doOnSubscribe(subscription -> timer.start())
				.doOnSuccess(s -> {
					long duration = timer.getMsSinceStart();
					metric.record(duration, TimeUnit.MILLISECONDS);
					logger.debug("{} {}ms", metric.getId().getName(), duration);
				});
	}

	public static <V> V timed(Callable<V> callable, Timer metric) {
		SystemTimer systemTimer = SystemTimer.create();
		try {
			systemTimer.start();
			V call = callable.call();
			long duration = systemTimer.getMsSinceStart();
			metric.record(duration, TimeUnit.MILLISECONDS);
			return call;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static void timed(Runnable runnable, Timer metric) {
		SystemTimer systemTimer = SystemTimer.create();
		try {
			systemTimer.start();
			runnable.run();
			long duration = systemTimer.getMsSinceStart();
			metric.record(duration, TimeUnit.MILLISECONDS);
			logger.info("Runnable timer duration={}", duration);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static <T> Maybe<T> timed(Maybe<T> reactor, Timer metric) {
		SystemTimer timer = SystemTimer.create();
		return reactor
				.doOnSubscribe(subscription -> timer.start())
				.doOnSuccess(s -> {
					long duration = timer.getMsSinceStart();
					metric.record(duration, TimeUnit.MILLISECONDS);
					logger.debug("{} {}ms", metric.getId().getName(), duration);
				});
	}
}
