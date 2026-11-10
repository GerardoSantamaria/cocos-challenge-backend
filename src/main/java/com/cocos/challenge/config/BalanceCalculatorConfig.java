package com.cocos.challenge.config;

import com.cocos.challenge.domain.service.BalanceCalculator;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BalanceCalculatorConfig {

	@Bean
	BalanceCalculator balanceCalculator() {
		return new BalanceCalculator();
	}

}
