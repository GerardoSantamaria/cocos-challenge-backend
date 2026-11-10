package com.cocos.challenge.infrastructure.persistence.repository;

import java.util.List;

import com.cocos.challenge.infrastructure.persistence.entity.OrderEntity;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<OrderEntity, Integer> {

	List<OrderEntity> findByUserIdOrderByPlacedAtAscIdAsc(Integer userId);

}
