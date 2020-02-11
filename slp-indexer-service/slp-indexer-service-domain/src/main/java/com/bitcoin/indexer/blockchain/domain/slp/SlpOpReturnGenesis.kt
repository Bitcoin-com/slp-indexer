package com.bitcoin.indexer.blockchain.domain.slp

import java.math.BigInteger


/**
 * Except for containing token details a GENESIS transaction works exactly like MINT
 *
 * @author akibabu
 */
data class SlpOpReturnGenesis(
        override val tokenType: SlpTokenType,
        override val tokenId: SlpTokenId,
        val ticker: String,
        val name: String,
        val decimals: Int,
        override val batonVout: Int?, // May be null in which case the baton is destroyed
        override val mintedAmount: BigInteger, // The minted amount received by vout[1] of this transaction
        val documentUri: String
) :
        SlpOpReturn(tokenType, SlpTransactionType.SEND, tokenId), SlpOpReturn.BatonAndMint {

    val toDetails by lazy { SlpTokenDetails(tokenId, ticker, name, decimals, documentUri, null) }

    companion object {

        fun create(tokenType: SlpTokenType, tokenId: SlpTokenId, chunks: List<ByteArray?>): SlpOpReturnGenesis? {
            validateChunkSize(chunks)
            val ticker = chunks[4]?.let { String(it).replace(Char.MIN_VALUE, ' ') } ?: ""
            val name = chunks[5]?.let { String(it).replace(Char.MIN_VALUE, ' ') } ?: ""
            val decimals = chunks[8]?.let { ByteUtils.toInt(validateDecimals(it)) } ?: return null
            val batonByte: Byte? = validateBatonVout(chunks[9]?.getOrNull(0))
            val mintedAmount = chunks[10]?.let { it }.let { UnsignedBigInteger.parseUnsigned(BigInteger(it)) } ?: return null
            val documentUri = chunks[6]?.let { String(it).replace(Char.MIN_VALUE, '0') } ?: ""
            val documentHash = chunks[7]?.let { String(validateDocumentHash(it)).replace(Char.MIN_VALUE, '0') } ?: ""
            return SlpOpReturnGenesis(tokenType, tokenId, ticker, name, decimals, batonByte?.toInt(), mintedAmount, documentUri);
        }

        private fun validateDecimals(chunk: ByteArray): ByteArray {
            if (chunk.isEmpty() || chunk.size > 1) {
                throw RuntimeException("Invalid decimals")
            }
            if (ByteUtils.toInt(chunk) > 9) {
                throw RuntimeException("Invalid decimals")
            }

            return chunk;
        }

        private fun validateBatonVout(batonByte: Byte?): Byte? {
            if (batonByte != null) {
                if (batonByte.toInt() == 0 || batonByte.toInt() == 1) {
                    throw RuntimeException("Invalid baton")
                }
            }
            return batonByte
        }

        private fun validateDocumentHash(chunk: ByteArray): ByteArray {
            if (chunk.size == 32 || chunk.isEmpty()) {
                return chunk
            }
            throw RuntimeException("Invalid documentHash")
        }

        private fun validateChunkSize(chunks: List<ByteArray?>) {
            if (chunks.size > 11) {
                throw RuntimeException("Invalid chunk size")
            }
        }
    }




    override fun toString(): String {
        return "SlpOpReturnGenesis(tokenType=$tokenType, tokenId=$tokenId, ticker='$ticker', name='$name', decimals=$decimals, batonVout=$batonVout, mintedAmount=$mintedAmount, documentUri='$documentUri')"
    }


}

