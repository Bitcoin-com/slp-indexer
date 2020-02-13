package com.bitcoin.indexer.blockchain.domain.slp

import java.math.BigInteger


/**
 * @author akibabu
 */
class SlpOpReturnMint(
        tokenType: SlpTokenType,
        tokenId: SlpTokenId,
        override val batonVout: Int?,
        override val mintedAmount: BigInteger
) : SlpOpReturn(tokenType, SlpTransactionType.SEND, tokenId), SlpOpReturn.BatonAndMint {

    companion object {

        fun create(tokenType: SlpTokenType, tokenId: SlpTokenId, chunks: List<ByteArray?>): SlpOpReturnMint? {
            validateChunkSize(chunks)
            if (chunks[5] != null && chunks[5]?.isEmpty() == false) {
                val batonByte: Byte? = chunks[5]?.let { validateBatonByte(it[0]) } ?: return null
                val mintedAmount = chunks[6]?.let { it }.let { UnsignedBigInteger.parseUnsigned(BigInteger(it)) }
                        ?: return null
                return SlpOpReturnMint(tokenType, tokenId, batonByte?.toInt(), mintedAmount)
            }
            val mintedAmount = chunks[6]?.let { it }.let { UnsignedBigInteger.parseUnsigned(BigInteger(it)) }
                    ?: return null
            return SlpOpReturnMint(tokenType, tokenId, null, mintedAmount)

        }

        private fun validateBatonByte(batonByte: Byte?): Byte? {
            if (batonByte != null) {
                if (batonByte.toInt() == 0 || batonByte.toInt() == 1) {
                    throw RuntimeException("Invalid baton")
                }
            }
            return batonByte
        }

        private fun validateChunkSize(chunks: List<ByteArray?>) {
            if (chunks.size > 7) {
                throw RuntimeException("Invalid chunk size")
            }
        }
    }

    override fun toString(): String {
        return "SlpOpReturnMint(batonVout=$batonVout, mintedAmount=$mintedAmount, tokenId=$tokenId)"
    }


}

