package com.bitcoin.indexer.blockchain.domain.slp

import java.io.Serializable


/**
 * @author akibabu
 */
class SlpTokenId(val hex: String): Serializable {

    companion object {
        fun tryParse(bytes: ByteArray): SlpTokenId? {
            if (bytes.size != 32) {
                return null
            }
            return SlpTokenId(ByteUtils.Hex.encode(bytes))
        }
    }

    override fun toString(): String {
        return hex
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SlpTokenId

        if (hex != other.hex) return false

        return true
    }

    override fun hashCode(): Int {
        return hex.hashCode()
    }


    val bytes: ByteArray by lazy { ByteUtils.Hex.decode(hex) }


}
