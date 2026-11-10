package com.cocos.challenge.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.cocos.challenge.domain.enums.OrderStatus;
import com.cocos.challenge.domain.enums.OrderType;
import com.cocos.challenge.domain.enums.Side;

public record PlacedOrder(Integer id, Integer instrumentId, Side side, OrderType type, Integer size, BigDecimal price,
		OrderStatus status, LocalDateTime datetime) {
}
