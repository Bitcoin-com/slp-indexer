package com.bitcoin.indexer.blockchain.domain.slp;

import static org.junit.Assert.assertThat;

import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptBuilder;
import org.bitcoinj.script.ScriptChunk;
import org.bitcoinj.script.ScriptOpCodes;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.spongycastle.util.encoders.Hex;

public class SlpOpReturnParserTest {

	@Test
	public void string_encoding() {
		byte[] first = new byte[] { 0x53, 0x4c, 0x50, 0x00 };
		byte[] second = new byte[] { 0x01 };
		byte[] third = new byte[] { 0x53, 0x45, 0x4e, (byte) 0xc4 };
		//"1388888888888888"
		byte[] txId = new byte[] {
				(byte) 0x88,
				(byte) 0x88,
				(byte) 0x88,
				(byte) 0x88,
				(byte) 0x88,
				(byte) 0x88,
				(byte) 0x88,
				0x6a,
				0x04,
				0x53,
				0x4c,
				0x50,
				0x00,
				0x01,
				0x00,
				(byte) 0x88,
				(byte) 0x88,
				(byte) 0x88,
				(byte) 0x88,
				(byte) 0x88,
				(byte) 0x88,
				(byte) 0x88,
				(byte) 0x88,
				0x00,
				0x13,
				(byte) 0x88,
				(byte) 0x88,
				(byte) 0x88,
				(byte) 0x88,
				(byte) 0x88,
				(byte) 0x88,
				(byte) 0x88,
		};
		byte[] fifth = new byte[] { 0x00, 0x00, 0x00,
				(byte) 0xeb, (byte) 0xff, (byte) 0xf9,
				0x21, 0x42
		};

		Script opreturn = new ScriptBuilder().op(ScriptOpCodes.OP_RETURN)
				.addChunk(new ScriptChunk(ScriptOpCodes.OP_PUSHDATA4, first))
				.addChunk(new ScriptChunk(ScriptOpCodes.OP_PUSHDATA1, second))
				.addChunk(new ScriptChunk(ScriptOpCodes.OP_PUSHDATA4, third))
				.addChunk(new ScriptChunk(32, txId))
				.addChunk(new ScriptChunk(8, fifth))
				.addChunk(new ScriptChunk(8, fifth))
				.build();

		String program = Hex.toHexString(opreturn.getProgram());

		SlpOpReturn slpOpReturn = SlpOpReturn.Companion.tryParse("888888888888886a04534c500001008888888888888888001388888888888888", program);

		assertThat(slpOpReturn, Matchers.nullValue());
	}

	@Test
	public void validator_discrepancy() {
		byte[] first = new byte[] { 0x53, 0x4c, 0x50, 0x00 };
		byte[] second = new byte[] { 0x41 };
		byte[] third = new byte[] { 0x53, 0x45, 0x4e, (byte) 0x44 };
		//"1388888888888888"
		byte[] txId = new byte[] {
				(byte) 0x88,
				(byte) 0x88,
				(byte) 0x88,
				(byte) 0x88,
				(byte) 0x88,
				(byte) 0x88,
				(byte) 0x88,
				0x6a,
				0x04,
				0x53,
				0x4c,
				0x50,
				0x00,
				0x01,
				0x00,
				(byte) 0x88,
				(byte) 0x88,
				(byte) 0x88,
				(byte) 0x88,
				(byte) 0x88,
				(byte) 0x88,
				(byte) 0x88,
				(byte) 0x88,
				0x00,
				0x13,
				(byte) 0x88,
				(byte) 0x88,
				(byte) 0x88,
				(byte) 0x88,
				(byte) 0x88,
				(byte) 0x88,
				(byte) 0x88,
		};
		byte[] fifth = new byte[] { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0x2c };


		byte[] sixth = new byte[] { 0x00, 0x00, 0x00, (byte) 0xb3, 0x00, (byte) 0xe0, 0x03, (byte) 0xe8 };


		Script opreturn = new ScriptBuilder().op(ScriptOpCodes.OP_RETURN)
				.addChunk(new ScriptChunk(ScriptOpCodes.OP_PUSHDATA4, first))
				.addChunk(new ScriptChunk(ScriptOpCodes.OP_PUSHDATA1, second))
				.addChunk(new ScriptChunk(ScriptOpCodes.OP_PUSHDATA4, third))
				.addChunk(new ScriptChunk(32, txId))
				.addChunk(new ScriptChunk(8, fifth))
				.addChunk(new ScriptChunk(8, sixth))
				.build();

		String program = Hex.toHexString(opreturn.getProgram());
		SlpOpReturn slpOpReturn = SlpOpReturn.Companion.tryParse("888888888888886a04534c500001008888888888888888001388888888888888", program);
		assertThat(slpOpReturn, Matchers.notNullValue());

	}

}
