package com.bitcoin.indexer.blockchain.domain.slp;

import java.io.Serializable;
import java.util.Objects;

public class SlpValid implements Serializable {
	private final String reason;
	private final Valid valid;

	public enum Valid {
		INVALID,
		VALID,
		UNKNOWN
	}

	private SlpValid(String reason, Valid valid) {
		this.reason = Objects.requireNonNull(reason);
		this.valid = Objects.requireNonNull(valid);
	}

	public static SlpValid create(String reason, Valid valid) {
		return new SlpValid(reason, valid);
	}

	public static SlpValid valid(String reason) {
		return new SlpValid(reason, Valid.VALID);
	}

	public static SlpValid invalid(String reason) {
		return new SlpValid(reason, Valid.INVALID);
	}

	public static SlpValid unknown() {
		return new SlpValid("NOT YET DETERMINED", Valid.UNKNOWN);
	}

	public String getReason() {
		return reason;
	}

	public Valid getValid() {
		return valid;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		SlpValid slpValid = (SlpValid) o;
		return valid.name().equals(slpValid.valid.name());
	}

	@Override
	public int hashCode() {
		return Objects.hash(valid.name());
	}

	@Override
	public String toString() {
		return "SlpValid [" +
				"reason=" + reason +
				", valid=" + valid +
				']';
	}
}
