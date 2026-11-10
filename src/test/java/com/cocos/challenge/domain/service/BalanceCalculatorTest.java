package com.cocos.challenge.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import com.cocos.challenge.domain.enums.OrderStatus;
import com.cocos.challenge.domain.enums.OrderType;
import com.cocos.challenge.domain.enums.Side;
import com.cocos.challenge.domain.model.AccountSnapshot;
import com.cocos.challenge.domain.model.Holding;
import com.cocos.challenge.domain.model.OrderView;

import org.junit.jupiter.api.Test;

class BalanceCalculatorTest {

	private final BalanceCalculator calculator = new BalanceCalculator();

	@Test
	void derivesCashReservationsAndHoldingsFromTheSeed() {
		AccountSnapshot snapshot = calculator.calculate(seedOrders());

		assertThat(snapshot.cashBalance()).isEqualByComparingTo("753000.00");
		assertThat(snapshot.reservedCash()).isEqualByComparingTo("125500.00");
		assertThat(snapshot.availableCash()).isEqualByComparingTo("627500.00");

		Holding pamp = snapshot.holdings().get(47);
		assertThat(pamp.quantity()).isEqualTo(40);
		assertThat(pamp.availableShares()).isEqualTo(40);
		assertThat(pamp.avgCostPrice()).isEqualByComparingTo("930");

		Holding metr = snapshot.holdings().get(54);
		assertThat(metr.quantity()).isEqualTo(500);
		assertThat(metr.availableShares()).isEqualTo(500);
		assertThat(metr.avgCostPrice()).isEqualByComparingTo("250");

		Holding bma = snapshot.holdings().get(31);
		assertThat(bma.quantity()).isEqualTo(-10);
		assertThat(bma.availableShares()).isEqualTo(-10);
		assertThat(snapshot.holdings()).doesNotContainKey(66);
	}

	@Test
	void cashMovementsUseSizeNotPrice() {
		AccountSnapshot snapshot = calculator
			.calculate(List.of(order(66, Side.CASH_IN, OrderType.MARKET, OrderStatus.FILLED, 100, "5"),
					order(66, Side.CASH_OUT, OrderType.MARKET, OrderStatus.FILLED, 40, "9")));

		assertThat(snapshot.cashBalance()).isEqualByComparingTo("60");
		assertThat(snapshot.holdings()).isEmpty();
	}

	@Test
	void openLimitOrdersReserveCashAndShares() {
		AccountSnapshot snapshot = calculator
			.calculate(List.of(order(66, Side.CASH_IN, OrderType.MARKET, OrderStatus.FILLED, 1_000, "1"),
					order(1, Side.BUY, OrderType.MARKET, OrderStatus.FILLED, 10, "10"),
					order(1, Side.BUY, OrderType.LIMIT, OrderStatus.NEW, 2, "10"),
					order(1, Side.SELL, OrderType.LIMIT, OrderStatus.NEW, 3, "12")));

		assertThat(snapshot.cashBalance()).isEqualByComparingTo("900");
		assertThat(snapshot.reservedCash()).isEqualByComparingTo("20");
		assertThat(snapshot.availableCash()).isEqualByComparingTo("880");

		Holding holding = snapshot.holdings().get(1);
		assertThat(holding.quantity()).isEqualTo(10);
		assertThat(holding.reservedShares()).isEqualTo(3);
		assertThat(holding.availableShares()).isEqualTo(7);
		assertThat(holding.avgCostPrice()).isEqualByComparingTo("10");
	}

	private static List<OrderView> seedOrders() {
		return List.of(order(66, Side.CASH_IN, OrderType.MARKET, OrderStatus.FILLED, 1_000_000, "1"),
				order(47, Side.BUY, OrderType.MARKET, OrderStatus.FILLED, 50, "930"),
				order(47, Side.BUY, OrderType.LIMIT, OrderStatus.CANCELLED, 50, "920"),
				order(47, Side.SELL, OrderType.MARKET, OrderStatus.FILLED, 10, "940"),
				order(45, Side.BUY, OrderType.LIMIT, OrderStatus.NEW, 50, "710"),
				order(47, Side.SELL, OrderType.MARKET, OrderStatus.REJECTED, 100, "950"),
				order(31, Side.BUY, OrderType.LIMIT, OrderStatus.NEW, 60, "1500"),
				order(66, Side.CASH_OUT, OrderType.MARKET, OrderStatus.FILLED, 100_000, "1"),
				order(31, Side.BUY, OrderType.LIMIT, OrderStatus.FILLED, 20, "1540"),
				order(54, Side.BUY, OrderType.MARKET, OrderStatus.FILLED, 500, "250"),
				order(31, Side.SELL, OrderType.MARKET, OrderStatus.FILLED, 30, "1530"));
	}

	private static OrderView order(int instrumentId, Side side, OrderType type, OrderStatus status, int size,
			String price) {
		return new OrderView(instrumentId, side, type, status, size, new BigDecimal(price));
	}

}
