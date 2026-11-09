package com.cocos.challenge.application;

import java.math.BigDecimal;

import com.cocos.challenge.domain.OrderType;
import com.cocos.challenge.domain.Side;

public record PlaceOrderCommand(
		int instrumentId,
		Side side,
		OrderType type,
		Integer size,
		BigDecimal amount,
		BigDecimal price) {
}
