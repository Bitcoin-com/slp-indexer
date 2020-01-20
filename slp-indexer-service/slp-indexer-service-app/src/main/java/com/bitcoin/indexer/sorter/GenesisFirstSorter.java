package com.bitcoin.indexer.sorter;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.bitcoinj.core.Transaction;

import com.bitcoin.indexer.blockchain.domain.slp.SlpOpReturn;
import com.bitcoin.indexer.blockchain.domain.slp.SlpOpReturnGenesis;
import com.bitcoin.indexer.facade.BitcoinJConverters;

public class GenesisFirstSorter {

	private List<Transaction> block;

	public GenesisFirstSorter(List<Transaction> block) {
		this.block = new ArrayList<>(block);
	}

	public List<Transaction> getSortedBlock() {
		LinkedList<Transaction> linkedList = new LinkedList<>();

		for (Transaction transaction : block) {
			List<SlpOpReturn> opReturn = BitcoinJConverters.getOpReturn(transaction);
			if (opReturn.isEmpty()) {
				linkedList.push(transaction);
				continue;
			}

			SlpOpReturn slpOpReturn = opReturn.get(0);
			if (slpOpReturn instanceof SlpOpReturnGenesis) {
				linkedList.push(transaction);
			} else {
				linkedList.addLast(transaction);
			}
		}
		return linkedList;
	}
}
