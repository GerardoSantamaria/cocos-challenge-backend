package com.cocos.challenge.api.dto.response;

import java.math.BigDecimal;

import com.cocos.challenge.domain.model.InstrumentMatch;

public record InstrumentResponse(int id, String ticker, String name, BigDecimal lastPrice, BigDecimal dailyReturnPct) {

	public static InstrumentResponse from(InstrumentMatch match) {
		return new InstrumentResponse(match.id(), match.ticker(), match.name(), match.lastPrice(),
				match.dailyReturnPct());
	}
}
