package com.cocos.challenge.domain;

import java.math.BigDecimal;

public record InstrumentMatch(
		int id,
		String ticker,
		String name,
		BigDecimal lastPrice,
		BigDecimal dailyReturnPct) {
}
