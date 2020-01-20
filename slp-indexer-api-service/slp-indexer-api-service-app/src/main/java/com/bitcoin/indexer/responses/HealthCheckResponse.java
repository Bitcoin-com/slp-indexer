package com.bitcoin.indexer.responses;

public class HealthCheckResponse {

	public String region;
	public String yourIp;

	public HealthCheckResponse() {
	}

	public HealthCheckResponse(String region, String yourIp) {
		this.region = region;
		this.yourIp = yourIp;
	}
}