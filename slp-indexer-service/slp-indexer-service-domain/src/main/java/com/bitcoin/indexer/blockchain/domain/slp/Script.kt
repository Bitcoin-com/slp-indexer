package com.bitcoin.indexer.blockchain.domain.slp

/**
 * @author akibabu
 */
internal class Script {

    private val script: org.bitcoinj.script.Script
    val bytes: ByteArray

    constructor(bytes: ByteArray) : this(org.bitcoinj.script.Script(bytes))

    constructor(script: org.bitcoinj.script.Script) {
        this.script = script
        this.bytes = script.program
    }

    val isOpReturn: Boolean
        get() = script.isOpReturn
    val chunks: List<ByteArray?>
        get() = script.chunks.map { it.data }

}
