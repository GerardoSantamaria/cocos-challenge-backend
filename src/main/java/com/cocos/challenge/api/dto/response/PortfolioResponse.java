package com.cocos.challenge.api.dto.response;

import java.math.BigDecimal;
import java.util.List;

import com.cocos.challenge.domain.model.Portfolio;

public record PortfolioResponse(BigDecimal availableCash, BigDecimal totalAccountValue,
		List<PositionResponse> positions) {

	public static PortfolioResponse from(Portfolio portfolio) {
		return new PortfolioResponse(portfolio.availableCash(), portfolio.totalAccountValue(),
				portfolio.positions().stream().map(PositionResponse::from).toList());
	}
}
