package com.cocos.challenge.api.controller;

import com.cocos.challenge.api.dto.request.PlaceOrderRequest;
import com.cocos.challenge.api.dto.response.OrderResponse;
import com.cocos.challenge.application.command.PlaceOrderCommand;
import com.cocos.challenge.application.service.CancelOrderService;
import com.cocos.challenge.application.service.PlaceOrderService;
import com.cocos.challenge.domain.enums.Side;
import com.cocos.challenge.domain.model.PlacedOrder;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users/{userId}/orders")
public class OrderController {

	private final PlaceOrderService placeOrderService;

	private final CancelOrderService cancelOrderService;

	public OrderController(PlaceOrderService placeOrderService, CancelOrderService cancelOrderService) {
		this.placeOrderService = placeOrderService;
		this.cancelOrderService = cancelOrderService;
	}

	@PostMapping
	public ResponseEntity<OrderResponse> place(@PathVariable int userId,
			@Valid @RequestBody PlaceOrderRequest request) {
		PlacedOrder order = placeOrderService.place(userId, toCommand(request));
		return ResponseEntity.status(HttpStatus.CREATED).body(OrderResponse.from(order));
	}

	@PostMapping("/{orderId}/cancel")
	public OrderResponse cancel(@PathVariable int userId, @PathVariable int orderId) {
		return OrderResponse.from(cancelOrderService.cancel(userId, orderId));
	}

	private static PlaceOrderCommand toCommand(PlaceOrderRequest request) {
		return new PlaceOrderCommand(request.instrumentId(), Side.valueOf(request.side().name()), request.type(),
				request.size(), request.amount(), request.price());
	}

}
