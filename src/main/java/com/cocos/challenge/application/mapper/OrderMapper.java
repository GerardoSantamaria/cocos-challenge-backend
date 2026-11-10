package com.cocos.challenge.application.mapper;

import com.cocos.challenge.domain.model.OrderView;
import com.cocos.challenge.domain.model.PlacedOrder;
import com.cocos.challenge.infrastructure.persistence.entity.OrderEntity;

public final class OrderMapper {

	private OrderMapper() {
	}

	public static OrderView toView(OrderEntity order) {
		return new OrderView(order.getInstrumentId(), order.getSide(), order.getType(), order.getStatus(),
				order.getSize(), order.getPrice());
	}

	public static PlacedOrder toPlacedOrder(OrderEntity order) {
		return new PlacedOrder(order.getId(), order.getInstrumentId(), order.getSide(), order.getType(),
				order.getSize(), order.getPrice(), order.getStatus(), order.getPlacedAt());
	}

}
