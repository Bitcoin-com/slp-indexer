package com.bitcoin.indexer.repository.db;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "current_height")
public class HeightDbObject {

	@Id
	private String key;

	private Long height;

	public HeightDbObject(Long height) {
		key = "CURRENT_HEIGHT";
		this.height = height;
	}

	public String getKey() {
		return key;
	}

	public Long getHeight() {
		return height;
	}
}
