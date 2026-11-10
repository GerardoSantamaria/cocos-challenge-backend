package com.cocos.challenge.api;

import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cocos.challenge.IntegrationTestSupport;

import org.junit.jupiter.api.Test;

class PortfolioApiTest extends IntegrationTestSupport {

	@Test
	void portfolioOfTheSeedUserMatchesTheAgreedFigures() throws Exception {
		mockMvc.perform(get("/api/v1/users/1/portfolio"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.availableCash", closeTo(627500.00, MONEY)))
			.andExpect(jsonPath("$.totalAccountValue", closeTo(904784.00, MONEY)))
			.andExpect(jsonPath("$.positions", hasSize(2)))
			.andExpect(jsonPath("$.positions[0].instrumentId").value(54))
			.andExpect(jsonPath("$.positions[0].ticker").value("METR"))
			.andExpect(jsonPath("$.positions[0].name").value("MetroGAS S.A."))
			.andExpect(jsonPath("$.positions[0].quantity").value(500))
			.andExpect(jsonPath("$.positions[0].lastPrice", closeTo(229.50, MONEY)))
			.andExpect(jsonPath("$.positions[0].marketValue", closeTo(114750.00, MONEY)))
			.andExpect(jsonPath("$.positions[0].avgCostPrice", closeTo(250.00, MONEY)))
			.andExpect(jsonPath("$.positions[0].totalReturnPct", closeTo(-8.20, MONEY)))
			.andExpect(jsonPath("$.positions[0].dailyReturnPct", closeTo(-1.08, MONEY)))
			.andExpect(jsonPath("$.positions[1].instrumentId").value(47))
			.andExpect(jsonPath("$.positions[1].ticker").value("PAMP"))
			.andExpect(jsonPath("$.positions[1].quantity").value(40))
			.andExpect(jsonPath("$.positions[1].lastPrice", closeTo(925.85, MONEY)))
			.andExpect(jsonPath("$.positions[1].marketValue", closeTo(37034.00, MONEY)))
			.andExpect(jsonPath("$.positions[1].avgCostPrice", closeTo(930.00, MONEY)))
			.andExpect(jsonPath("$.positions[1].totalReturnPct", closeTo(-0.45, MONEY)))
			.andExpect(jsonPath("$.positions[1].dailyReturnPct", closeTo(0.44, MONEY)));
	}

	@Test
	void portfolioOfAUserWithoutOrdersIsEmpty() throws Exception {
		mockMvc.perform(get("/api/v1/users/2/portfolio"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.availableCash", closeTo(0, MONEY)))
			.andExpect(jsonPath("$.totalAccountValue", closeTo(0, MONEY)))
			.andExpect(jsonPath("$.positions", hasSize(0)));
	}

	@Test
	void unknownUserPortfolioIsNotFound() throws Exception {
		mockMvc.perform(get("/api/v1/users/99/portfolio")).andExpect(status().isNotFound());
	}

}
