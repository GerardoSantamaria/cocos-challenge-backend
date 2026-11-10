package com.cocos.challenge.api;

import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cocos.challenge.IntegrationTestSupport;

import org.junit.jupiter.api.Test;

class InstrumentSearchApiTest extends IntegrationTestSupport {

	private static final String SEARCH = "/api/v1/instruments/search";

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
