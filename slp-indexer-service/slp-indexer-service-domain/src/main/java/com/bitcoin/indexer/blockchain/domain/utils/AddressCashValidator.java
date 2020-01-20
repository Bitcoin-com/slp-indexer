package com.bitcoin.indexer.blockchain.domain.utils;

import org.bitcoinj.core.AddressFormatException;

/**
 * From BitcoinJ
 */
class AddressCashValidator {

    static void checkNonEmptyPayload(byte[] payload) {
        if (payload.length == 0) {
            throw new AddressFormatException("No payload");
        }
    }

    static void checkAllowedPadding(byte extraBits) {
        if (extraBits >= 5) {
            throw new AddressFormatException("More than allowed padding");
        }
    }

    static void checkNonZeroPadding(byte last, byte mask) {
        if ((last & mask) != 0) {
            throw new AddressFormatException("Nonzero padding bytes");
        }
    }

    static void checkFirstBitIsZero(byte versionByte) {
        if ((versionByte & 0x80) != 0) {
            throw new AddressFormatException("First bit is reserved");
        }
    }

    static void checkDataLength(byte[] data, int hashSize) {
        if (data.length != hashSize + 1) {
            throw new AddressFormatException("Data length " + data.length + " != hash size " + hashSize);
        }
    }

}