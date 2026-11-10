package com.cocos.challenge.application.service;

import java.util.List;
import java.util.Map;

import com.cocos.challenge.application.support.LikePatterns;
import com.cocos.challenge.application.support.QuoteLookup;
import com.cocos.challenge.domain.model.InstrumentMatch;
import com.cocos.challenge.domain.support.MoneyMath;
import com.cocos.challenge.infrastructure.persistence.entity.InstrumentEntity;
import com.cocos.challenge.infrastructure.persistence.entity.MarketDataEntity;
import com.cocos.challenge.infrastructure.persistence.repository.InstrumentRepository;
import com.cocos.challenge.infrastructure.persistence.repository.MarketDataRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InstrumentSearchService {

	private static final int MAX_RESULTS = 20;

	private final InstrumentRepository instrumentRepository;

	private final MarketDataRepository marketDataRepository;

	public InstrumentSearchService(InstrumentRepository instrumentRepository,
			MarketDataRepository marketDataRepository) {
		this.instrumentRepository = instrumentRepository;
		this.marketDataRepository = marketDataRepository;
	}

	@Transactional(readOnly = true)
	public List<InstrumentMatch> search(String query) {
		List<InstrumentEntity> instruments = instrumentRepository.search(LikePatterns.contains(query.trim()),
				MAX_RESULTS);
		Map<Integer, MarketDataEntity> quotes = QuoteLookup.latest(marketDataRepository,
				instruments.stream().map(InstrumentEntity::getId).toList());
		return instruments.stream().map(instrument -> toMatch(instrument, quotes.get(instrument.getId()))).toList();
	}

	private static InstrumentMatch toMatch(InstrumentEntity instrument, MarketDataEntity quote) {
		if (quote == null) {
			return new InstrumentMatch(instrument.getId(), instrument.getTicker(), instrument.getName(), null, null);
		}
		return new InstrumentMatch(instrument.getId(), instrument.getTicker(), instrument.getName(),
				MoneyMath.money(quote.getClosePrice()),
				MoneyMath.percentChange(quote.getClosePrice(), quote.getPreviousClose()));
	}

}
