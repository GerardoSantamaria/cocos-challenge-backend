package com.cocos.challenge.domain.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.cocos.challenge.domain.enums.OrderStatus;
import com.cocos.challenge.domain.enums.OrderType;
import com.cocos.challenge.domain.enums.Side;
import com.cocos.challenge.domain.model.AccountSnapshot;
import com.cocos.challenge.domain.model.Holding;
import com.cocos.challenge.domain.model.OrderView;
import com.cocos.challenge.domain.support.MoneyMath;

public final class BalanceCalculator {

	/** Expects {@code orders} in chronological order: the average cost depends on it. */
	public AccountSnapshot calculate(List<OrderView> orders) {
		BigDecimal cash = BigDecimal.ZERO;
		BigDecimal reservedCash = BigDecimal.ZERO;
		Map<Integer, Accumulator> byInstrument = new HashMap<>();

		for (OrderView order : orders) {
			if (order.status() == OrderStatus.FILLED) {
				cash = cash.add(cashEffect(order));
				applyFilledPosition(order, byInstrument);
			}
			else if (isOpenLimit(order)) {
				applyReservation(order, byInstrument);
				if (order.side() == Side.BUY) {
					reservedCash = reservedCash.add(notional(order));
				}
			}
		}

		Map<Integer, Holding> holdings = new HashMap<>();
		byInstrument.forEach((instrumentId, accumulator) -> holdings.put(instrumentId, accumulator.toHolding()));
		return new AccountSnapshot(cash, reservedCash, cash.subtract(reservedCash), holdings);
	}

	private static boolean isOpenLimit(OrderView order) {
		return order.status() == OrderStatus.NEW && order.type() == OrderType.LIMIT
				&& (order.side() == Side.BUY || order.side() == Side.SELL);
	}

	private static BigDecimal cashEffect(OrderView order) {
		return switch (order.side()) {
			case CASH_IN -> shares(order);
			case CASH_OUT -> shares(order).negate();
			case BUY -> notional(order).negate();
			case SELL -> notional(order);
		};
	}

	private static void applyFilledPosition(OrderView order, Map<Integer, Accumulator> byInstrument) {
		if (order.side() != Side.BUY && order.side() != Side.SELL) {
			return;
		}
		Accumulator accumulator = byInstrument.computeIfAbsent(order.instrumentId(), ignored -> new Accumulator());
		if (order.side() == Side.BUY) {
			accumulator.addBuy(order.size(), order.price());
		}
		else {
			accumulator.addSell(order.size());
		}
	}

	private static void applyReservation(OrderView order, Map<Integer, Accumulator> byInstrument) {
		if (order.side() != Side.SELL) {
			return;
		}
		byInstrument.computeIfAbsent(order.instrumentId(), ignored -> new Accumulator()).reserveSell(order.size());
	}

	private static BigDecimal notional(OrderView order) {
		requireSizeAndPrice(order);
		return BigDecimal.valueOf(order.size()).multiply(order.price());
	}

	private static BigDecimal shares(OrderView order) {
		if (order.size() == null) {
			throw new IllegalStateException("Filled cash movement is missing size");
		}
		return BigDecimal.valueOf(order.size());
	}

	private static void requireSizeAndPrice(OrderView order) {
		if (order.size() == null || order.price() == null) {
			throw new IllegalStateException("Order is missing size or price");
		}
	}

	private static final class Accumulator {

		private long quantity;

		private long reservedSell;

		private BigDecimal costBasis = BigDecimal.ZERO;

		private void addBuy(int size, BigDecimal price) {
			if (price == null) {
				throw new IllegalStateException("Filled buy is missing price");
			}
			if (quantity >= 0) {
				costBasis = costBasis.add(BigDecimal.valueOf(size).multiply(price));
				quantity += size;
				return;
			}
			quantity += size;
			costBasis = quantity > 0 ? BigDecimal.valueOf(quantity).multiply(price) : BigDecimal.ZERO;
		}

		private void addSell(int size) {
			if (quantity <= 0) {
				quantity -= size;
				return;
			}
			long remaining = quantity - size;
			costBasis = remaining > 0
					? costBasis.multiply(BigDecimal.valueOf(remaining))
						.divide(BigDecimal.valueOf(quantity), MoneyMath.DIVISION_SCALE, RoundingMode.HALF_UP)
					: BigDecimal.ZERO;
			quantity = remaining;
		}

		private void reserveSell(int size) {
			reservedSell += size;
		}

		private Holding toHolding() {
			long available = quantity - reservedSell;
			BigDecimal avgCost = quantity > 0
					? costBasis.divide(BigDecimal.valueOf(quantity), MoneyMath.DIVISION_SCALE, RoundingMode.HALF_UP)
					: null;
			return new Holding(toInt(quantity), toInt(reservedSell), toInt(available), avgCost);
		}

		private static int toInt(long value) {
			if (value > Integer.MAX_VALUE || value < Integer.MIN_VALUE) {
				throw new IllegalStateException("Position quantity is outside the supported range");
			}
			return (int) value;
		}

	}

}
