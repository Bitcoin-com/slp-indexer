package com.bitcoin.indexer.facade.validators;

import java.util.List;

import com.bitcoin.indexer.blockchain.domain.Input;
import com.bitcoin.indexer.blockchain.domain.Utxo;
import com.bitcoin.indexer.blockchain.domain.slp.SlpValid;

public interface SlpValidatorFacade {

	SlpValid isValid(String txId, String tokenId, String tokenType, List<Utxo> utxos, List<Input> inputs, SlpValidatorFacade baseValidator);

}
