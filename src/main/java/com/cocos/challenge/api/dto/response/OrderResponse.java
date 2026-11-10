package com.cocos.challenge.api.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.cocos.challenge.domain.enums.OrderStatus;
import com.cocos.challenge.domain.enums.OrderType;
import com.cocos.challenge.domain.enums.Side;
import com.cocos.challenge.domain.model.PlacedOrder;

public record OrderResponse(Integer id, Integer instrumentId, Side side, OrderType type, Integer size, BigDecimal price,
		OrderStatus status, LocalDateTime datetime) {

	public static OrderResponse from(PlacedOrder order) {
		return new OrderResponse(order.id(), order.instrumentId(), order.side(), order.type(), order.size(),
				order.price(), order.status(), order.datetime());
	}
}
