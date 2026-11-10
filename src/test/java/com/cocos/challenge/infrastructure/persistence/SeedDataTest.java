package com.cocos.challenge.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.cocos.challenge.IntegrationTestSupport;
import com.cocos.challenge.infrastructure.persistence.repository.InstrumentRepository;
import com.cocos.challenge.infrastructure.persistence.repository.MarketDataRepository;
import com.cocos.challenge.infrastructure.persistence.repository.OrderRepository;
import com.cocos.challenge.infrastructure.persistence.repository.UserRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class SeedDataTest extends IntegrationTestSupport {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private InstrumentRepository instrumentRepository;

	@Autowired
	private OrderRepository orderRepository;

	@Autowired
	private MarketDataRepository marketDataRepository;

	@Test
	void seedCountsMatchTheOfficialDump() {
		assertThat(userRepository.count()).isEqualTo(4);
		assertThat(instrumentRepository.count()).isEqualTo(66);
		assertThat(orderRepository.count()).isEqualTo(11);
		assertThat(marketDataRepository.count()).isEqualTo(126);
	}

}
