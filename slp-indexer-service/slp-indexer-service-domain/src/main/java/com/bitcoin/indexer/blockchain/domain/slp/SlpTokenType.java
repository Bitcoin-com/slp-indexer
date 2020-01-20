package com.bitcoin.indexer.blockchain.domain.slp;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class SlpTokenType {
	private final String type;
	private final byte[] bytes;

	public static final SlpTokenType PERMISSIONLESS = new SlpTokenType("PERMISSIONLESS", String.valueOf(1).getBytes());
	public static final SlpTokenType NFT1_GENESIS = new SlpTokenType("NFT1_GENESIS", String.valueOf(129).getBytes());
	public static final SlpTokenType NFT1_CHILD = new SlpTokenType("NFT1_CHILD", String.valueOf(65).getBytes());

	private static final Map<Integer, String> currentKnownTokens =
			Map.of(1, "PERMISSIONLESS",
					129, "NFT1_GENESIS",
					65, "NFT1_CHILD");

	private SlpTokenType(String type, byte[] bytes) {
		this.type = Objects.requireNonNull(type);
		this.bytes = bytes;
	}

	public static Optional<SlpTokenType> tryParse(byte[] bytes) {
		if (bytes.length == 0 || bytes.length > 2) {
			return Optional.empty();
		}

		int key = ByteUtils.INSTANCE.toInt(bytes);
		if (!currentKnownTokens.containsKey(key)) {
			return Optional.of(new SlpTokenType("UNKNOWN", bytes));
		}
		return Optional.of(new SlpTokenType(currentKnownTokens.get(key), bytes));
	}

	public String getType() {
		return type;
	}

	public byte[] getBytes() {
		return bytes;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		SlpTokenType that = (SlpTokenType) o;
		return Objects.equals(type, that.type) &&
				Arrays.equals(bytes, that.bytes);
	}

	@Override
	public int hashCode() {
		int result = Objects.hash(type);
		result = 31 * result + Arrays.hashCode(bytes);
		return result;
	}
}
