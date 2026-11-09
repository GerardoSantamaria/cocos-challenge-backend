package com.cocos.challenge.application;

import com.cocos.challenge.domain.BalanceCalculator;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BalanceCalculatorConfig {

	@Bean
	BalanceCalculator balanceCalculator() {
		return new BalanceCalculator();
	}
}
