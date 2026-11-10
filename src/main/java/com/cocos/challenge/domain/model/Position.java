package com.cocos.challenge.domain.model;

import java.math.BigDecimal;

public record Position(int instrumentId, String ticker, String name, int quantity, BigDecimal lastPrice,
		BigDecimal marketValue, BigDecimal avgCostPrice, BigDecimal totalReturnPct, BigDecimal dailyReturnPct) {
}
