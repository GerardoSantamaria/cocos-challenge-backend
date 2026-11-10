package com.cocos.challenge.domain.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class MoneyMathTest {

	@Test
	void moneyRoundsHalfUpToTwoDecimals() {
		assertThat(MoneyMath.money(new BigDecimal("925.855"))).isEqualTo(new BigDecimal("925.86"));
		assertThat(MoneyMath.money(new BigDecimal("925.854"))).isEqualTo(new BigDecimal("925.85"));
		assertThat(MoneyMath.money(new BigDecimal("100"))).isEqualTo(new BigDecimal("100.00"));
		assertThat(MoneyMath.money(null)).isNull();
	}

	@Test
	void percentChangeIsRoundedToTwoDecimals() {
		assertThat(MoneyMath.percentChange(new BigDecimal("229.50"), new BigDecimal("250.00")))
			.isEqualTo(new BigDecimal("-8.20"));
		assertThat(MoneyMath.percentChange(new BigDecimal("925.85"), new BigDecimal("930.00")))
			.isEqualTo(new BigDecimal("-0.45"));
	}

	@Test
	void percentChangeIsUndefinedWithoutABase() {
		assertThat(MoneyMath.percentChange(new BigDecimal("10"), BigDecimal.ZERO)).isNull();
		assertThat(MoneyMath.percentChange(new BigDecimal("10"), null)).isNull();
		assertThat(MoneyMath.percentChange(null, new BigDecimal("10"))).isNull();
	}

}
