package com.bitcoin.indexer.blockchain.domain.slp

import java.math.BigDecimal

/**
 * @author akibabu
 */
data class SlpTokenDetails(
        val tokenId: SlpTokenId,
        val ticker: String,
        val name: String,
        val decimals: Int,
        val documentUri: String,
        val verifiedToken: SlpVerifiedToken?) {

    /**
     * Example with 6 decimals: 12.53 -> 12530000
     */
    fun toRawAmount(amount: BigDecimal): Long {
        if (amount > maxRawAmount) {
            throw IllegalArgumentException("amount larger than 8 unsigned bytes")
        } else if (amount.scale() > decimals) {
            throw IllegalArgumentException("$ticker supports maximum $decimals decimals but amount is $amount")
        }
        return amount.scaleByPowerOfTen(decimals).toLong()
    }

    /**
     * Example with 6 decimals: 12530000 -> 12.53
     */
    fun toReadableAmount(rawAmount: BigDecimal): BigDecimal {
        return rawAmount.scaleByPowerOfTen(-decimals).stripTrailingZeros()
    }

    companion object {
        val maxRawAmount = BigDecimal(Long.MAX_VALUE.toString())
    }

}
