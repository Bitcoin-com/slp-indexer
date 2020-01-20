package com.bitcoin.indexer.blockchain.domain.slp

import org.spongycastle.util.encoders.DecoderException
import java.nio.ByteBuffer

/**
 * @author akibabu
 */
object ByteUtils {

    fun toLong(bytes: ByteArray): Long {
        return ByteBuffer.wrap(bytes).long
    }
    fun toLong(bytes: ByteArray?): Long? {
        return bytes?.let { ByteBuffer.wrap(it).long }
    }

    fun toInt(bytes: ByteArray): Int {
        val buffer = ByteBuffer.allocate(Int.SIZE_BYTES)
            .position(Int.SIZE_BYTES - bytes.size)
            as ByteBuffer
        buffer.put(bytes)
        return buffer.getInt(0)
    }

    object Hex {

        fun encode(bytes: ByteArray): String {
            return org.spongycastle.util.encoders.Hex.toHexString(bytes)
        }

        fun decode(hex: String): ByteArray {
            try {
                return org.spongycastle.util.encoders.Hex.decode(hex)
            } catch (e: DecoderException) {
                throw RuntimeException(e)
            }
        }
    }

    object Base58 {
        fun encode(input: ByteArray): String {
            return org.bitcoinj.core.Base58.encode(input)
        }
    }

}
