package com.bitcoin.indexer.repository.db;

import org.bson.Document;

import com.bitcoin.indexer.blockchain.domain.slp.SlpValid;
import com.bitcoin.indexer.blockchain.domain.slp.SlpValid.Valid;

public class SlpValidDbType {

	private String reason;
	private Valid valid;

	public SlpValidDbType(String reason, Valid valid) {
		this.reason = reason;
		this.valid = valid;
	}

	public static SlpValidDbType fromDomain(SlpValid slpValid) {
		return new SlpValidDbType(slpValid.getReason(), slpValid.getValid());
	}

	public SlpValidDbType() {
	}

	public String getReason() {
		return reason;
	}

	public Valid getValid() {
		return valid;
	}

	public Document toDocument() {
		Document document = new Document();
		document.put("reason", reason);
		document.put("valid", valid.name());
		return document;
	}
}
