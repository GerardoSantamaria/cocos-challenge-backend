package com.cocos.challenge.domain;

import java.math.BigDecimal;

public record Holding(int quantity, int reservedShares, int availableShares, BigDecimal avgCostPrice) {
}
