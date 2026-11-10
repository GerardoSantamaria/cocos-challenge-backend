package com.cocos.challenge.domain.model;

import java.math.BigDecimal;

import com.cocos.challenge.domain.enums.OrderStatus;
import com.cocos.challenge.domain.enums.OrderType;
import com.cocos.challenge.domain.enums.Side;

public record OrderView(int instrumentId, Side side, OrderType type, OrderStatus status, Integer size,
		BigDecimal price) {
}
