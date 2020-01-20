package com.bitcoin.indexer.responses;

public class Output {
	public String address;
	public String amount;

	public Output(String address, String amount) {
		this.address = address;
		this.amount = amount;
	}
}
