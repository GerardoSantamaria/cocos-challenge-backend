package com.cocos.challenge.domain;

import java.math.BigDecimal;
import java.util.Map;

public record AccountSnapshot(
		BigDecimal cashBalance,
		BigDecimal reservedCash,
		BigDecimal availableCash,
		Map<Integer, Holding> holdings) {

	public AccountSnapshot {
		holdings = Map.copyOf(holdings);
	}

	public int availableShares(int instrumentId) {
		Holding holding = holdings.get(instrumentId);
		return holding == null ? 0 : holding.availableShares();
	}
}
