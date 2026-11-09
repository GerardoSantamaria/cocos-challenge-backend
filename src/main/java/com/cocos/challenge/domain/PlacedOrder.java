package com.cocos.challenge.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PlacedOrder(
		Integer id,
		Integer instrumentId,
		Side side,
		OrderType type,
		Integer size,
		BigDecimal price,
		OrderStatus status,
		LocalDateTime datetime) {
}
