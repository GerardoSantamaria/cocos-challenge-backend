package com.cocos.challenge.domain.support;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class MoneyMath {

	public static final int DIVISION_SCALE = 8;

	private static final int MONEY_SCALE = 2;

	private MoneyMath() {
	}

	public static BigDecimal money(BigDecimal value) {
		if (value == null) {
			return null;
		}
		return value.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
	}

	public static BigDecimal percentChange(BigDecimal current, BigDecimal base) {
		if (current == null || base == null || base.signum() == 0) {
			return null;
		}
		return current.subtract(base)
			.divide(base, DIVISION_SCALE, RoundingMode.HALF_UP)
			.movePointRight(2)
			.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
	}

}
