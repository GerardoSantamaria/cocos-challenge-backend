package com.cocos.challenge.application.service;

import com.cocos.challenge.application.mapper.OrderMapper;
import com.cocos.challenge.domain.enums.OrderStatus;
import com.cocos.challenge.domain.exception.ConflictException;
import com.cocos.challenge.domain.exception.NotFoundException;
import com.cocos.challenge.domain.model.PlacedOrder;
import com.cocos.challenge.infrastructure.persistence.entity.OrderEntity;
import com.cocos.challenge.infrastructure.persistence.repository.OrderRepository;
import com.cocos.challenge.infrastructure.persistence.repository.UserRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CancelOrderService {

	private static final Logger log = LoggerFactory.getLogger(CancelOrderService.class);

	private final UserRepository userRepository;

	private final OrderRepository orderRepository;

	public CancelOrderService(UserRepository userRepository, OrderRepository orderRepository) {
		this.userRepository = userRepository;
		this.orderRepository = orderRepository;
	}

	@Transactional
	public PlacedOrder cancel(int userId, int orderId) {
		userRepository.lockById(userId).orElseThrow(() -> new NotFoundException("User not found"));
		OrderEntity order = orderRepository.findById(orderId)
			.orElseThrow(() -> new NotFoundException("Order not found"));
		if (order.getUserId() == null || order.getUserId() != userId) {
			throw new NotFoundException("Order not found");
		}
		if (order.getStatus() != OrderStatus.NEW) {
			throw new ConflictException("Only orders in NEW status can be cancelled");
		}
		order.setStatus(OrderStatus.CANCELLED);
		OrderEntity saved = orderRepository.saveAndFlush(order);
		log.info("Order cancelled userId={} orderId={}", userId, saved.getId());
		return OrderMapper.toPlacedOrder(saved);
	}

}
