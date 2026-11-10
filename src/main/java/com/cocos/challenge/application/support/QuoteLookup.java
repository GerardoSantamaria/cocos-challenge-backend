package com.cocos.challenge.application.support;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.cocos.challenge.infrastructure.persistence.entity.MarketDataEntity;
import com.cocos.challenge.infrastructure.persistence.repository.MarketDataRepository;

public final class QuoteLookup {

	private QuoteLookup() {
	}

	public static Map<Integer, MarketDataEntity> latest(MarketDataRepository repository,
			Collection<Integer> instrumentIds) {
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
