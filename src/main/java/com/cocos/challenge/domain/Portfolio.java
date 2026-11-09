package com.cocos.challenge.domain;

import java.math.BigDecimal;
import java.util.List;

public record Portfolio(
		BigDecimal availableCash,
		BigDecimal totalAccountValue,
		List<Position> positions) {
}
