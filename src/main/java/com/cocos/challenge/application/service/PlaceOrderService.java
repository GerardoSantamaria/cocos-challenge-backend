package com.cocos.challenge.application.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import com.cocos.challenge.application.command.PlaceOrderCommand;
import com.cocos.challenge.application.mapper.OrderMapper;
import com.cocos.challenge.domain.enums.InstrumentType;
import com.cocos.challenge.domain.enums.OrderStatus;
import com.cocos.challenge.domain.enums.OrderType;
import com.cocos.challenge.domain.enums.Side;
import com.cocos.challenge.domain.exception.InvalidOrderException;
import com.cocos.challenge.domain.exception.NotFoundException;
import com.cocos.challenge.domain.model.AccountSnapshot;
import com.cocos.challenge.domain.model.OrderView;
import com.cocos.challenge.domain.model.PlacedOrder;
import com.cocos.challenge.domain.service.BalanceCalculator;
import com.cocos.challenge.domain.support.MoneyMath;
import com.cocos.challenge.infrastructure.persistence.entity.InstrumentEntity;
import com.cocos.challenge.infrastructure.persistence.entity.MarketDataEntity;
import com.cocos.challenge.infrastructure.persistence.entity.OrderEntity;
import com.cocos.challenge.infrastructure.persistence.repository.InstrumentRepository;
import com.cocos.challenge.infrastructure.persistence.repository.MarketDataRepository;
import com.cocos.challenge.infrastructure.persistence.repository.OrderRepository;
import com.cocos.challenge.infrastructure.persistence.repository.UserRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlaceOrderService {

	private static final Logger log = LoggerFactory.getLogger(PlaceOrderService.class);

	private final UserRepository userRepository;

	private final InstrumentRepository instrumentRepository;

	private final OrderRepository orderRepository;

	private final MarketDataRepository marketDataRepository;

	private final BalanceCalculator balanceCalculator;

	private final Clock clock;

	public PlaceOrderService(UserRepository userRepository, InstrumentRepository instrumentRepository,
			OrderRepository orderRepository, MarketDataRepository marketDataRepository,
			BalanceCalculator balanceCalculator, Clock clock) {
		this.userRepository = userRepository;
		this.instrumentRepository = instrumentRepository;
		this.orderRepository = orderRepository;
		this.marketDataRepository = marketDataRepository;
		this.balanceCalculator = balanceCalculator;
		this.clock = clock;
	}

	@Transactional
	public PlacedOrder place(int userId, PlaceOrderCommand command) {
		if (command.side() != Side.BUY && command.side() != Side.SELL) {
			throw new InvalidOrderException("Only BUY and SELL orders are accepted");
		}
		userRepository.lockById(userId).orElseThrow(() -> new NotFoundException("User not found"));
		InstrumentEntity instrument = instrumentRepository.findById(command.instrumentId())
			.orElseThrow(() -> new NotFoundException("Instrument not found"));
		if (instrument.getType() == InstrumentType.MONEDA) {
			throw new InvalidOrderException("Orders on currency instruments are not allowed");
		}

		BigDecimal price = resolvePrice(command);
		Integer size = resolveSize(command, price);
		if (price == null || size == null || size == 0) {
			return persist(userId, command, size, price, OrderStatus.REJECTED);
		}

		AccountSnapshot balances = balanceCalculator.calculate(loadOrders(userId));
		boolean accepted = switch (command.side()) {
			case BUY -> cost(size, price).compareTo(balances.availableCash()) <= 0;
			case SELL -> size <= balances.availableShares(command.instrumentId());
			default -> false;
		};
		return persist(userId, command, size, price, accepted ? acceptedStatus(command) : OrderStatus.REJECTED);
	}

	private BigDecimal resolvePrice(PlaceOrderCommand command) {
		if (command.type() == OrderType.LIMIT) {
			return MoneyMath.money(command.price());
		}
		return marketDataRepository
			.findFirstByInstrumentIdAndClosePriceIsNotNullOrderByMarketDateDescIdDesc(command.instrumentId())
			.map(MarketDataEntity::getClosePrice)
			.filter(close -> close.signum() > 0)
			.map(MoneyMath::money)
			.orElse(null);
	}

	private static Integer resolveSize(PlaceOrderCommand command, BigDecimal price) {
		if (command.size() != null) {
			return command.size();
		}
		if (price == null) {
			return null;
		}
		try {
			return command.amount().divide(price, 0, RoundingMode.FLOOR).intValueExact();
		}
		catch (ArithmeticException ex) {
			throw new InvalidOrderException("Computed size exceeds the allowed range");
		}
	}

	private static OrderStatus acceptedStatus(PlaceOrderCommand command) {
		return command.type() == OrderType.MARKET ? OrderStatus.FILLED : OrderStatus.NEW;
	}

	private static BigDecimal cost(int size, BigDecimal price) {
		return BigDecimal.valueOf(size).multiply(price);
	}

	private List<OrderView> loadOrders(int userId) {
		return orderRepository.findByUserId(userId).stream().map(OrderMapper::toView).toList();
	}

	private PlacedOrder persist(int userId, PlaceOrderCommand command, Integer size, BigDecimal price,
			OrderStatus status) {
		OrderEntity entity = new OrderEntity();
		entity.setUserId(userId);
		entity.setInstrumentId(command.instrumentId());
		entity.setSide(command.side());
		entity.setType(command.type());
		entity.setSize(size);
		entity.setPrice(price);
		entity.setStatus(status);
		entity.setPlacedAt(LocalDateTime.now(clock));
		OrderEntity saved = orderRepository.saveAndFlush(entity);
		log.info("Order persisted userId={} orderId={} status={}", userId, saved.getId(), status);
		return OrderMapper.toPlacedOrder(saved);
	}

}
