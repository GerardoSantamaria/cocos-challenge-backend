package com.cocos.challenge.domain.model;

import java.math.BigDecimal;
import java.util.List;

public record Portfolio(BigDecimal availableCash, BigDecimal totalAccountValue, List<Position> positions) {
}
