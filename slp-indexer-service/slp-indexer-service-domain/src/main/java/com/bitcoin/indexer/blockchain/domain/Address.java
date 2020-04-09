package com.bitcoin.indexer.blockchain.domain;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.bitcoinj.params.MainNetParams;

import com.bitcoin.indexer.blockchain.domain.utils.AddressCashUtil;
import com.bitcoin.indexer.blockchain.domain.utils.AddressCashUtil.AddressVersionAndBytes;

public class Address implements Serializable {
	private static final long serialVersionUID = 1L;
	private final String address;
	private Type type;

	public enum Type {
		COINBASE, UNKNOWN, NORMAL, OPRETURN
	}

	private Address(String address, Type type) {
		this.address = Objects.requireNonNull(address);
		this.type = Objects.requireNonNull(type);
	}

	public static Address create(String address) {
		return new Address(address, Type.NORMAL);
	}

	public static Address coinbase(String txId) {
		return new Address(txId + "-" + UUID.randomUUID().toString() + "-COINBASE", Type.COINBASE);
	}

	public static Address opReturn(String txId) {
		return new Address(txId + "-" + UUID.randomUUID().toString() + "-OPRETURN", Type.OPRETURN);
	}

	public static Address unknownFormat() {
		return new Address(UUID.randomUUID().toString() + "-UNKNOWN", Type.UNKNOWN);
	}

	public static Address slpToBase58(String address) {
		List<String> parts = Arrays.asList(address.split(":"));
		String prefix = "simpleledger";
		if (parts.size() != 2 || !parts.get(0).equals(prefix)) {
			throw new IllegalArgumentException("Not SLP address=" + address);
		}
		AddressVersionAndBytes decode = AddressCashUtil.decode(prefix, address);
		String base58 = new org.bitcoinj.core.Address(MainNetParams.get(), decode.getVersion(), decode.getBytes()).toBase58();
		return new Address(base58, Type.NORMAL);
	}

	public static Address toBase58(String address) {
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

	public static Address cashAddressToBase58(String address) {
		List<String> parts = Arrays.asList(address.split(":"));
		String prefix = "bitcoincash";
		if (parts.size() != 2 || !parts.get(0).equals(prefix)) {
			throw new IllegalArgumentException("Not SLP address=" + address);
		}
		AddressVersionAndBytes decode = AddressCashUtil.decode(prefix, address);
		String base58 = new org.bitcoinj.core.Address(MainNetParams.get(), decode.getVersion(), decode.getBytes()).toBase58();
		return new Address(base58, Type.NORMAL);
	}

	public static Address base58ToSlp(String base58) {
		String prefix = "simpleledger";
		org.bitcoinj.core.Address address = org.bitcoinj.core.Address.fromBase58(MainNetParams.get(), base58);

		String slpAddress = AddressCashUtil.encodeCashAddress(prefix,
				AddressCashUtil.packAddressData(address.getHash160(), Integer.valueOf(address.getVersion()).byteValue()));

		return new Address(slpAddress, Type.NORMAL);
	}

	public static Address base58ToCash(String base58) {
		String prefix = "bitcoincash";
		org.bitcoinj.core.Address address = org.bitcoinj.core.Address.fromBase58(MainNetParams.get(), base58);

		String slpAddress = AddressCashUtil.encodeCashAddress(prefix,
				AddressCashUtil.packAddressData(address.getHash160(), Integer.valueOf(address.getVersion()).byteValue()));

		return new Address(slpAddress, Type.NORMAL);
	}

	public String getAddress() {
		return address;
	}

	@Override
	public String toString() {
		return address;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		Address address1 = (Address) o;
		return Objects.equals(address, address1.address);
	}

	@Override
	public int hashCode() {
		return Objects.hash(address);
	}

	public Type getType() {
		return type;
	}

	public boolean isOpReturn() {
		return address.contains("OPRETURN");
	}
}