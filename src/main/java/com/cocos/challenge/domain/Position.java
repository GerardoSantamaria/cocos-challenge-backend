package com.cocos.challenge.domain;

import java.math.BigDecimal;

public record Position(
		int instrumentId,
		String ticker,
		String name,
		int quantity,
		BigDecimal lastPrice,
		BigDecimal marketValue,
		BigDecimal avgCostPrice,
		BigDecimal totalReturnPct,
		BigDecimal dailyReturnPct) {
}
