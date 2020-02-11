package com.bitcoin.indexer.facade;

import static org.hamcrest.MatcherAssert.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.bitcoin.indexer.blockchain.domain.slp.SlpOpReturn;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import io.micrometer.core.instrument.util.IOUtils;

@RunWith(Parameterized.class)
public class SLPDBScriptsParserTest {

	private final String msg;
	private final String script;
	private final Integer code;

	//This test will be ugly because of the non-mapping models of slpdb and this indexer
	@Test()
	public void parameterized_validation() {
		System.out.println(msg);

		SlpOpReturn slpOpReturn = SlpOpReturn.Companion.tryParse("6448381f9649ecacd8c30189cfbfee71a91b6b9738ea494fe33f8b8b51cbfca0", script);
		if (code == null) {
			assertThat(slpOpReturn, Matchers.notNullValue());
		} else {
			assertThat(slpOpReturn, Matchers.nullValue());
		}

	}

	@Parameters(name = "{index}: {0}")
	public static Collection<Object[]> data() {
		JsonParser jsonParser = new JsonParser();
		List<Object[]> list = new ArrayList<>();

		for (JsonElement jsonElement : jsonParser.parse(testCases).getAsJsonArray()) {
			String msg = jsonElement.getAsJsonObject().get("msg").getAsString();
			String script = jsonElement.getAsJsonObject().get("script").getAsString();
			Integer code = null;
			if (!jsonElement.getAsJsonObject().get("code").isJsonNull()) {
				code = jsonElement.getAsJsonObject().get("code").getAsInt();
			}
			list.add(new Object[] { msg, script, code });
		}
		return list;
	}

	public static String testCases;

	static {
		testCases = IOUtils.toString(SLPDBTestVectorTest.class.getClassLoader().getResourceAsStream("scripts_tests.json"), StandardCharsets.UTF_8);
	}

	public SLPDBScriptsParserTest(String msg, String script, Integer code) {
		this.msg = msg;
		this.script = script;
		this.code = code;
	}

}
