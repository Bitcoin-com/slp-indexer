package com.bitcoin.indexer;

import java.util.List;

import com.bitcoin.indexer.blockchain.domain.Input;
import com.bitcoin.indexer.blockchain.domain.Utxo;
import com.bitcoin.indexer.blockchain.domain.slp.SlpValid;
import com.bitcoin.indexer.facade.validators.SlpValidatorFacade;

public class FakeSlpValidator implements SlpValidatorFacade {

	private boolean valid;

	public FakeSlpValidator(boolean valid) {
		this.valid = valid;
	}

	@Override
	public SlpValid isValid(String txId, String tokenId, String tokenType, List<Utxo> utxos, List<Input> inputs, SlpValidatorFacade baseValidator) {
		if (valid) {
			return SlpValid.valid("Always valid");
		}

		return SlpValid.invalid("Always invalid");
	}

	public void setValid(boolean valid) {
		this.valid = valid;
	}

}
