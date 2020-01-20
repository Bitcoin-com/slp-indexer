package com.bitcoin.indexer.blockchain.domain.slp;

import java.math.BigInteger;

public class UnsignedBigInteger {

	private static final BigInteger TWO_COMPL_REF = BigInteger.ONE.shiftLeft(64);
	//To support unsigned integers from the token
	public static BigInteger parseUnsigned(BigInteger b) {
		if (b.compareTo(BigInteger.ZERO) < 0) {
			b = b.add(TWO_COMPL_REF);
		}

		byte[] unsignedbyteArray = b.toByteArray();
		return new BigInteger(unsignedbyteArray);
	}

}
