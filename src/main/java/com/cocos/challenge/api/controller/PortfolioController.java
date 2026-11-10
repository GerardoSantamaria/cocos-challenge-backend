package com.cocos.challenge.api.controller;

import com.cocos.challenge.api.dto.response.PortfolioResponse;
import com.cocos.challenge.application.service.PortfolioService;

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
	public PortfolioResponse portfolio(@PathVariable int userId) {
		return PortfolioResponse.from(portfolioService.portfolio(userId));
	}

}
