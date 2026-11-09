package com.cocos.challenge.application;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.cocos.challenge.infrastructure.persistence.MarketDataEntity;
import com.cocos.challenge.infrastructure.persistence.MarketDataRepository;

final class QuoteLookup {

	private QuoteLookup() {
	}

	static Map<Integer, MarketDataEntity> latest(MarketDataRepository repository, Collection<Integer> instrumentIds) {
		if (instrumentIds.isEmpty()) {
			return Map.of();
		}
		Map<Integer, MarketDataEntity> quotes = new HashMap<>();
		for (MarketDataEntity quote : repository.findLatest(instrumentIds)) {
			quotes.merge(quote.getInstrumentId(), quote, QuoteLookup::preferLater);
		}
		return quotes;
	}

	private static MarketDataEntity preferLater(MarketDataEntity left, MarketDataEntity right) {
		int byDate = left.getMarketDate().compareTo(right.getMarketDate());
		if (byDate > 0) {
			return left;
		}
		if (byDate < 0) {
			return right;
		}
		return left.getId() >= right.getId() ? left : right;
	}
}
