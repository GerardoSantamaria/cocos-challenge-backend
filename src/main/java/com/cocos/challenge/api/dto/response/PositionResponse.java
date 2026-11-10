package com.cocos.challenge.api.dto.response;

import java.math.BigDecimal;

import com.cocos.challenge.domain.model.Position;

public record PositionResponse(int instrumentId, String ticker, String name, int quantity, BigDecimal lastPrice,
		BigDecimal marketValue, BigDecimal avgCostPrice, BigDecimal totalReturnPct, BigDecimal dailyReturnPct) {

	public static PositionResponse from(Position position) {
		return new PositionResponse(position.instrumentId(), position.ticker(), position.name(), position.quantity(),
				position.lastPrice(), position.marketValue(), position.avgCostPrice(), position.totalReturnPct(),
				position.dailyReturnPct());
	}
}
