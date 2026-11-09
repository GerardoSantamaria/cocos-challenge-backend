package com.cocos.challenge.api;

import com.cocos.challenge.application.PlaceOrderCommand;
import com.cocos.challenge.application.CancelOrderService;
import com.cocos.challenge.application.PlaceOrderService;
import com.cocos.challenge.domain.PlacedOrder;
import com.cocos.challenge.domain.Side;

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
	public ResponseEntity<PlacedOrder> place(@PathVariable int userId, @Valid @RequestBody PlaceOrderRequest request) {
		PlacedOrder order = placeOrderService.place(userId, new PlaceOrderCommand(
				request.instrumentId(),
				Side.valueOf(request.side().name()),
				request.type(),
				request.size(),
				request.amount(),
				request.price()));
		return ResponseEntity.status(HttpStatus.CREATED).body(order);
	}

	@PostMapping("/{orderId}/cancel")
	public PlacedOrder cancel(@PathVariable int userId, @PathVariable int orderId) {
		return cancelOrderService.cancel(userId, orderId);
	}
}
