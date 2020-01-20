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
            val ticker = chunks[4]?.let { String(it) } ?: ""
            val name = chunks[5]?.let { String(it) } ?: ""
            val decimals = chunks[8]?.let { ByteUtils.toInt(it) } ?: return null
            val batonByte: Byte? = chunks[9]?.let { it.getOrNull(0) }
            val mintedAmount = chunks[10]?.let { it }.let { UnsignedBigInteger.parseUnsigned(BigInteger(it)) } ?: return null
            val documentUri = chunks[6]?.let { String(it) } ?: ""
            return SlpOpReturnGenesis(tokenType, tokenId, ticker, name, decimals, batonByte?.toInt(), mintedAmount, documentUri);
        }
    }

    override fun toString(): String {
        return "SlpOpReturnGenesis(tokenType=$tokenType, tokenId=$tokenId, ticker='$ticker', name='$name', decimals=$decimals, batonVout=$batonVout, mintedAmount=$mintedAmount, documentUri='$documentUri')"
    }


}

