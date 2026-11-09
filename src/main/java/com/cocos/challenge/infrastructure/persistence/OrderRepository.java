package com.cocos.challenge.infrastructure.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<OrderEntity, Integer> {

	List<OrderEntity> findByUserId(Integer userId);
}
