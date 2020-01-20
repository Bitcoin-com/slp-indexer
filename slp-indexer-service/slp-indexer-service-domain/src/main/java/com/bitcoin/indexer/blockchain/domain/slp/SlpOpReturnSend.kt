package com.bitcoin.indexer.blockchain.domain.slp

import org.bitcoinj.script.ScriptBuilder
import org.bitcoinj.script.ScriptChunk
import org.bitcoinj.script.ScriptOpCodes
import java.math.BigInteger
import java.nio.ByteBuffer

/**
 * @author akibabu
 */
class SlpOpReturnSend(tokenType: SlpTokenType, tokenId: SlpTokenId, val quantities: List<BigInteger>) :
        SlpOpReturn(tokenType, SlpTransactionType.SEND, tokenId) {

    init {
        if (quantities.isEmpty() || quantities.size > MAX_QUANTITIES) {
            throw IllegalArgumentException("SLP SEND with ${quantities.size} quantities")
        }
    }

    companion object {
        private const val MAX_CHUNKS_SEND = MAX_QUANTITIES + 5

        fun create(tokenType: SlpTokenType, tokenId: SlpTokenId, chunks: List<ByteArray?>): SlpOpReturnSend? {
            if (chunks.size > MAX_CHUNKS_SEND) {
                return null
            }
            val quantities = chunks
                    .filter { chunk -> chunk?.size == 8 }
                    .map { UnsignedBigInteger.parseUnsigned(BigInteger(it!!)) }
                    .map { it }

            return try {
                SlpOpReturnSend(tokenType, tokenId, quantities)
            } catch (e: IllegalArgumentException) {
                null
            }
        }
    }

    fun createScript(): org.bitcoinj.script.Script {
        val builder = ScriptBuilder()
                .op(ScriptOpCodes.OP_RETURN)
                .data(LOKAD)
                // .data() would not figure out this is 1 byte push-data
                .addChunk(ScriptChunk(tokenType.bytes.size, tokenType.bytes))
                .data(transactionType.bytes)
                .data(tokenId.bytes)
        quantities.forEach { builder.data(ByteBuffer.allocate(Long.SIZE_BYTES).putLong(it.longValueExact()).array()) }
        return builder.build()
    }

    override fun toString(): String {
        return "SlpOpReturnSend(quantities=$quantities, tokenId=$tokenId)"
    }


}

