package com.cocos.challenge.api;

import java.math.BigDecimal;

import com.cocos.challenge.domain.OrderType;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@ValidPlaceOrder
public record PlaceOrderRequest(
		@NotNull @Positive Integer instrumentId,
		@NotNull TradeSide side,
		@NotNull OrderType type,
		@Positive Integer size,
		@Positive @Digits(integer = 8, fraction = 2) BigDecimal amount,
		@Positive @Digits(integer = 8, fraction = 2) BigDecimal price) {
}
