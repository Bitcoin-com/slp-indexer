package com.bitcoin.indexer.facade;

import java.util.List;

public class InsightsResponse {
	public String txid;
	public int version;
	public int locktime;
	public List<Vin> vin;
	public List<Vout> vout;
	public String blockhash;
	public Integer blockheight;
	public Integer confirmations;
	public Long time;
	public Long blocktime;
	public String valueOut;
	public Long size;
	public String valueIn;
	public String fees;

	public static class Vin {
		public String txid;
		public int vout;
		public long sequence;
		public int n;
		public String value;
		public String legacyAddress;
		public String cashAddress;

	}

	public static class Vout {
		public String value;
		public int n;
		public ScriptPubKey scriptPubKey;
		public String spentTxId;
		public Integer spentIndex;
		public Integer spentHeight;

	}

	public static class ScriptSig {
		public String hex;
		public String asm;
	}

	public static class ScriptPubKey {
		public String hex;
		public String asm;
		public List<String> addresses;
		public String type;
		public List<String> cashAddrs;

	}
}
