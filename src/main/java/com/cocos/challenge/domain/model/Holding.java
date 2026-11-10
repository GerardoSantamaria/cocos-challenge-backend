package com.cocos.challenge.domain.model;

import java.math.BigDecimal;

public record Holding(int quantity, int reservedShares, int availableShares, BigDecimal avgCostPrice) {
}
