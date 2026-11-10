package com.cocos.challenge.application.command;

import java.math.BigDecimal;

import com.cocos.challenge.domain.enums.OrderType;
import com.cocos.challenge.domain.enums.Side;

public record PlaceOrderCommand(int instrumentId, Side side, OrderType type, Integer size, BigDecimal amount,
		BigDecimal price) {
}
