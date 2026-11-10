package com.cocos.challenge.api;

import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import com.cocos.challenge.IntegrationTestSupport;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class InstrumentSearchApiTest extends IntegrationTestSupport {

	private static final String SEARCH = "/api/v1/instruments/search";

	@Autowired
	private JdbcTemplate jdbc;

	@Test
	void latestPriceSkipsDaysWithoutAClose() throws Exception {
		jdbc.update("insert into marketdata (instrumentid, date, close, previousclose) values (?, ?, null, null)", 47,
				LocalDate.of(2023, 7, 15));

		mockMvc.perform(get(SEARCH).param("q", "pamp"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].lastPrice", closeTo(925.85, MONEY)))
			.andExpect(jsonPath("$[0].dailyReturnPct", closeTo(0.44, MONEY)));

		mockMvc.perform(get("/api/v1/users/1/portfolio"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.positions[1].ticker").value("PAMP"))
			.andExpect(jsonPath("$.positions[1].lastPrice", closeTo(925.85, MONEY)));

		postOrder(1, marketBuy(47, 1)).andExpect(status().isCreated())
			.andExpect(jsonPath("$.status").value("FILLED"))
			.andExpect(jsonPath("$.price", closeTo(925.85, MONEY)));
	}

	@Test
	void searchMatchesTickerOrName() throws Exception {
		mockMvc.perform(get(SEARCH).param("q", "pamp"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(1)))
			.andExpect(jsonPath("$[0].ticker").value("PAMP"))
			.andExpect(jsonPath("$[0].lastPrice", closeTo(925.85, MONEY)))
			.andExpect(jsonPath("$[0].dailyReturnPct", closeTo(0.44, MONEY)));
	}

	@Test
	void searchExcludesCurrency() throws Exception {
		mockMvc.perform(get(SEARCH).param("q", "PESOS"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(0)));
	}

	@Test
	void likeWildcardsAreMatchedLiterally() throws Exception {
		mockMvc.perform(get(SEARCH).param("q", "%")).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
	}

	@Test
	void blankQueryIsABadRequest() throws Exception {
		mockMvc.perform(get(SEARCH).param("q", " ")).andExpect(status().isBadRequest());
	}

}
