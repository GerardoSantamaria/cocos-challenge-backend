package com.cocos.challenge.api;

import com.cocos.challenge.application.PortfolioService;
import com.cocos.challenge.domain.Portfolio;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users/{userId}/portfolio")
public class PortfolioController {

	private final PortfolioService portfolioService;

	public PortfolioController(PortfolioService portfolioService) {
		this.portfolioService = portfolioService;
	}

	@GetMapping
	public Portfolio portfolio(@PathVariable int userId) {
		return portfolioService.portfolio(userId);
	}
}
