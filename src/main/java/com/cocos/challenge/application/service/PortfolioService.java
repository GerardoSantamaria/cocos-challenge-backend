package com.cocos.challenge.application.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.cocos.challenge.application.mapper.OrderMapper;
import com.cocos.challenge.application.support.QuoteLookup;
import com.cocos.challenge.domain.enums.InstrumentType;
import com.cocos.challenge.domain.exception.NotFoundException;
import com.cocos.challenge.domain.model.AccountSnapshot;
import com.cocos.challenge.domain.model.Holding;
import com.cocos.challenge.domain.model.OrderView;
import com.cocos.challenge.domain.model.Portfolio;
import com.cocos.challenge.domain.model.Position;
import com.cocos.challenge.domain.service.BalanceCalculator;
import com.cocos.challenge.domain.support.MoneyMath;
import com.cocos.challenge.infrastructure.persistence.entity.InstrumentEntity;
import com.cocos.challenge.infrastructure.persistence.entity.MarketDataEntity;
import com.cocos.challenge.infrastructure.persistence.repository.InstrumentRepository;
import com.cocos.challenge.infrastructure.persistence.repository.MarketDataRepository;
import com.cocos.challenge.infrastructure.persistence.repository.OrderRepository;
import com.cocos.challenge.infrastructure.persistence.repository.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PortfolioService {

	private final UserRepository userRepository;

	private final OrderRepository orderRepository;

	private final InstrumentRepository instrumentRepository;

	private final MarketDataRepository marketDataRepository;

	private final BalanceCalculator balanceCalculator;

	public PortfolioService(UserRepository userRepository, OrderRepository orderRepository,
			InstrumentRepository instrumentRepository, MarketDataRepository marketDataRepository,
			BalanceCalculator balanceCalculator) {
		this.userRepository = userRepository;
		this.orderRepository = orderRepository;
		this.instrumentRepository = instrumentRepository;
		this.marketDataRepository = marketDataRepository;
		this.balanceCalculator = balanceCalculator;
	}

	@Transactional(readOnly = true)
	public Portfolio portfolio(int userId) {
		if (!userRepository.existsById(userId)) {
			throw new NotFoundException("User not found");
		}
		List<OrderView> orders = orderRepository.findByUserIdOrderByPlacedAtAscIdAsc(userId)
			.stream()
			.map(OrderMapper::toView)
			.toList();
		AccountSnapshot snapshot = balanceCalculator.calculate(orders);
		List<Integer> heldIds = snapshot.holdings()
			.entrySet()
			.stream()
			.filter(entry -> entry.getValue().quantity() > 0)
			.map(Map.Entry::getKey)
			.toList();

		Map<Integer, InstrumentEntity> instruments = heldIds.isEmpty() ? Map.of()
				: instrumentRepository.findAllById(heldIds)
					.stream()
					.collect(Collectors.toMap(InstrumentEntity::getId, Function.identity()));
		Map<Integer, MarketDataEntity> quotes = QuoteLookup.latest(marketDataRepository, heldIds);

		BigDecimal total = snapshot.cashBalance();
		List<Position> positions = new ArrayList<>();
		for (Integer instrumentId : heldIds) {
			InstrumentEntity instrument = instruments.get(instrumentId);
			if (instrument == null || instrument.getType() != InstrumentType.ACCIONES) {
				continue;
			}
			Position position = toPosition(instrument, snapshot.holdings().get(instrumentId), quotes.get(instrumentId));
			if (position.marketValue() != null) {
				total = total.add(position.marketValue());
			}
			positions.add(position);
		}
		positions.sort(Comparator.comparing(Position::ticker).thenComparingInt(Position::instrumentId));
		return new Portfolio(MoneyMath.money(snapshot.availableCash()), MoneyMath.money(total), List.copyOf(positions));
	}

	private static Position toPosition(InstrumentEntity instrument, Holding holding, MarketDataEntity quote) {
		BigDecimal lastPrice = quote == null ? null : MoneyMath.money(quote.getClosePrice());
		BigDecimal marketValue = lastPrice == null ? null
				: MoneyMath.money(lastPrice.multiply(BigDecimal.valueOf(holding.quantity())));
		BigDecimal avgCost = MoneyMath.money(holding.avgCostPrice());
		BigDecimal dailyReturn = quote == null ? null
				: MoneyMath.percentChange(quote.getClosePrice(), quote.getPreviousClose());
		return new Position(instrument.getId(), instrument.getTicker(), instrument.getName(), holding.quantity(),
				lastPrice, marketValue, avgCost, MoneyMath.percentChange(lastPrice, avgCost), dailyReturn);
	}

}
