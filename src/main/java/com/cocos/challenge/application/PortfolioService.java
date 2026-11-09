package com.cocos.challenge.application;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.cocos.challenge.domain.AccountSnapshot;
import com.cocos.challenge.domain.BalanceCalculator;
import com.cocos.challenge.domain.Holding;
import com.cocos.challenge.domain.InstrumentType;
import com.cocos.challenge.domain.OrderView;
import com.cocos.challenge.domain.Portfolio;
import com.cocos.challenge.domain.Position;
import com.cocos.challenge.domain.exception.NotFoundException;
import com.cocos.challenge.infrastructure.persistence.InstrumentEntity;
import com.cocos.challenge.infrastructure.persistence.InstrumentRepository;
import com.cocos.challenge.infrastructure.persistence.MarketDataEntity;
import com.cocos.challenge.infrastructure.persistence.MarketDataRepository;
import com.cocos.challenge.infrastructure.persistence.OrderRepository;
import com.cocos.challenge.infrastructure.persistence.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PortfolioService {

	private final UserRepository userRepository;
	private final OrderRepository orderRepository;
	private final InstrumentRepository instrumentRepository;
	private final MarketDataRepository marketDataRepository;
	private final BalanceCalculator balanceCalculator;

	public PortfolioService(
			UserRepository userRepository,
			OrderRepository orderRepository,
			InstrumentRepository instrumentRepository,
			MarketDataRepository marketDataRepository,
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
		List<OrderView> orders = orderRepository.findByUserId(userId).stream()
				.map(order -> new OrderView(
						order.getInstrumentId(),
						order.getSide(),
						order.getType(),
						order.getStatus(),
						order.getSize(),
						order.getPrice()))
				.toList();
		AccountSnapshot snapshot = balanceCalculator.calculate(orders);
		List<Integer> heldIds = snapshot.holdings().entrySet().stream()
				.filter(entry -> entry.getValue().quantity() > 0)
				.map(Map.Entry::getKey)
				.toList();

		Map<Integer, InstrumentEntity> instruments = heldIds.isEmpty()
				? Map.of()
				: instrumentRepository.findAllById(heldIds).stream()
						.collect(Collectors.toMap(InstrumentEntity::getId, Function.identity()));
		Map<Integer, MarketDataEntity> quotes = QuoteLookup.latest(marketDataRepository, heldIds);

		BigDecimal total = snapshot.cashBalance();
		List<Position> positions = new ArrayList<>();
		for (Integer instrumentId : heldIds) {
			InstrumentEntity instrument = instruments.get(instrumentId);
			if (instrument == null || instrument.getType() != InstrumentType.ACCIONES) {
				continue;
			}
			Holding holding = snapshot.holdings().get(instrumentId);
			MarketDataEntity quote = quotes.get(instrumentId);
			BigDecimal lastPrice = quote == null ? null : money(quote.getClosePrice());
			BigDecimal marketValue = lastPrice == null ? null : money(lastPrice.multiply(BigDecimal.valueOf(holding.quantity())));
			if (marketValue != null) {
				total = total.add(marketValue);
			}
			BigDecimal avgCost = holding.avgCostPrice() == null ? null : money(holding.avgCostPrice());
			positions.add(new Position(
					instrumentId,
					instrument.getTicker(),
					instrument.getName(),
					holding.quantity(),
					lastPrice,
					marketValue,
					avgCost,
					percentChange(lastPrice, avgCost),
					quote == null ? null : percentChange(quote.getClosePrice(), quote.getPreviousClose())));
		}
		positions.sort(Comparator.comparing(Position::ticker).thenComparingInt(Position::instrumentId));
		return new Portfolio(money(snapshot.availableCash()), money(total), List.copyOf(positions));
	}

	private static BigDecimal money(BigDecimal value) {
		if (value == null) {
			return null;
		}
		return value.setScale(2, RoundingMode.HALF_UP);
	}

	private static BigDecimal percentChange(BigDecimal current, BigDecimal base) {
		if (current == null || base == null || base.signum() == 0) {
			return null;
		}
		return current.subtract(base)
				.divide(base, BalanceCalculator.DIVISION_SCALE, RoundingMode.HALF_UP)
				.movePointRight(2)
				.setScale(2, RoundingMode.HALF_UP);
	}
}
