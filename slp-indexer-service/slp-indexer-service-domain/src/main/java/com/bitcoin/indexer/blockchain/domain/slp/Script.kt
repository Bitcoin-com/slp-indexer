package com.bitcoin.indexer.blockchain.domain.slp

import org.bitcoinj.script.ScriptOpCodes

/**
 * @author akibabu
 */
internal class Script {

    private val script: org.bitcoinj.script.Script
    val bytes: ByteArray

    constructor(bytes: ByteArray) : this(org.bitcoinj.script.Script(bytes))

    constructor(script: org.bitcoinj.script.Script) {
        validateScript(script)
        this.script = script
        this.bytes = script.program
    }

    private fun validateScript(script: org.bitcoinj.script.Script) {
        script.chunks.forEach{
            if (it.opcode == ScriptOpCodes.OP_0) {
                throw RuntimeException("Invalid script")
            }
        }


    }

    val isOpReturn: Boolean
        get() = script.isOpReturn
    val chunks: List<ByteArray?>
        get() = script.chunks.map { it.data }

}
