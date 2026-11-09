package com.cocos.challenge.domain;

import java.math.BigDecimal;

public record OrderView(
		int instrumentId,
		Side side,
		OrderType type,
		OrderStatus status,
		Integer size,
		BigDecimal price) {
}
