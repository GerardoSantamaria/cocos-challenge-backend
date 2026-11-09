package com.cocos.challenge.application;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

import com.cocos.challenge.domain.BalanceCalculator;
import com.cocos.challenge.domain.InstrumentMatch;
import com.cocos.challenge.infrastructure.persistence.InstrumentEntity;
import com.cocos.challenge.infrastructure.persistence.InstrumentRepository;
import com.cocos.challenge.infrastructure.persistence.MarketDataEntity;
import com.cocos.challenge.infrastructure.persistence.MarketDataRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InstrumentSearchService {

	static final int MAX_RESULTS = 20;

	private final InstrumentRepository instrumentRepository;
	private final MarketDataRepository marketDataRepository;

	public InstrumentSearchService(InstrumentRepository instrumentRepository, MarketDataRepository marketDataRepository) {
		this.instrumentRepository = instrumentRepository;
		this.marketDataRepository = marketDataRepository;
	}

	@Transactional(readOnly = true)
	public List<InstrumentMatch> search(String query) {
		List<InstrumentEntity> instruments = instrumentRepository.search(
				LikePatterns.contains(query.trim()),
				MAX_RESULTS);
		Map<Integer, MarketDataEntity> quotes = QuoteLookup.latest(
				marketDataRepository,
				instruments.stream().map(InstrumentEntity::getId).toList());
		return instruments.stream()
				.map(instrument -> toMatch(instrument, quotes.get(instrument.getId())))
				.toList();
	}

	private static InstrumentMatch toMatch(InstrumentEntity instrument, MarketDataEntity quote) {
		return new InstrumentMatch(
				instrument.getId(),
				instrument.getTicker(),
				instrument.getName(),
				quote == null || quote.getClosePrice() == null ? null : quote.getClosePrice().setScale(2, RoundingMode.HALF_UP),
				quote == null ? null : percent(quote));
	}

	private static BigDecimal percent(MarketDataEntity quote) {
		BigDecimal close = quote.getClosePrice();
		BigDecimal previous = quote.getPreviousClose();
		if (close == null || previous == null || previous.signum() == 0) {
			return null;
		}
		return close.subtract(previous)
				.divide(previous, BalanceCalculator.DIVISION_SCALE, RoundingMode.HALF_UP)
				.movePointRight(2)
				.setScale(2, RoundingMode.HALF_UP);
	}
}
