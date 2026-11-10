package com.cocos.challenge.api;

import static org.hamcrest.Matchers.closeTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cocos.challenge.IntegrationTestSupport;

import org.junit.jupiter.api.Test;

class CancelOrderApiTest extends IntegrationTestSupport {

	@Test
	void cancelReleasesTheReservedCashOfANewOrder() throws Exception {
		mockMvc.perform(post("/api/v1/users/1/orders/5/cancel"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(5))
			.andExpect(jsonPath("$.status").value("CANCELLED"));

		mockMvc.perform(get("/api/v1/users/1/portfolio"))
			.andExpect(jsonPath("$.availableCash", closeTo(663000.00, MONEY)))
			.andExpect(jsonPath("$.totalAccountValue", closeTo(904784.00, MONEY)));
	}

	@Test
	void filledOrdersCannotBeCancelled() throws Exception {
		mockMvc.perform(post("/api/v1/users/1/orders/2/cancel")).andExpect(status().isConflict());
	}

	@Test
	void aUserCannotCancelSomeoneElsesOrder() throws Exception {
		mockMvc.perform(post("/api/v1/users/2/orders/5/cancel")).andExpect(status().isNotFound());
	}

}
