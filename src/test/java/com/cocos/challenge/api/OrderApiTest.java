package com.cocos.challenge.api;

import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cocos.challenge.IntegrationTestSupport;

import org.junit.jupiter.api.Test;

class OrderApiTest extends IntegrationTestSupport {

	@Test
	void orderForAnUnknownUserIsNotFound() throws Exception {
		postOrder(99, marketBuy(47, 1)).andExpect(status().isNotFound());
	}

	@Test
	void unknownInstrumentIsNotFound() throws Exception {
		postOrder(1, marketBuy(99, 1)).andExpect(status().isNotFound());
	}

	@Test
	void marketBuyFillsAtTheLatestClose() throws Exception {
		postOrder(1, marketBuy(47, 1)).andExpect(status().isCreated())
			.andExpect(jsonPath("$.status").value("FILLED"))
			.andExpect(jsonPath("$.side").value("BUY"))
			.andExpect(jsonPath("$.type").value("MARKET"))
			.andExpect(jsonPath("$.size").value(1))
			.andExpect(jsonPath("$.price", closeTo(925.85, MONEY)))
			.andExpect(jsonPath("$.instrumentId").value(47))
			.andExpect(jsonPath("$.datetime").value("2026-11-09T18:00:00"));
	}

	@Test
	void amountBuysTheLargestWholeNumberOfShares() throws Exception {
		postOrder(1, """
				{"instrumentId":47,"side":"BUY","type":"MARKET","amount":2000}
				""").andExpect(status().isCreated())
			.andExpect(jsonPath("$.status").value("FILLED"))
			.andExpect(jsonPath("$.size").value(2))
			.andExpect(jsonPath("$.price", closeTo(925.85, MONEY)));
	}

	@Test
	void amountBelowThePriceIsRejected() throws Exception {
		postOrder(1, """
				{"instrumentId":47,"side":"BUY","type":"MARKET","amount":100}
				""").andExpect(status().isCreated())
			.andExpect(jsonPath("$.status").value("REJECTED"))
			.andExpect(jsonPath("$.size").value(0))
			.andExpect(jsonPath("$.price", closeTo(925.85, MONEY)));
	}

	@Test
	void buyThatNeedsReservedCashIsRejected() throws Exception {
		postOrder(1, marketBuy(47, 700)).andExpect(status().isCreated())
			.andExpect(jsonPath("$.status").value("REJECTED"))
			.andExpect(jsonPath("$.size").value(700));
	}

	@Test
	void limitBuyReservesCashWithoutChangingTheAccountValue() throws Exception {
		postOrder(1, """
				{"instrumentId":1,"side":"BUY","type":"LIMIT","size":1,"price":100}
				""").andExpect(status().isCreated())
			.andExpect(jsonPath("$.status").value("NEW"))
			.andExpect(jsonPath("$.price", closeTo(100, MONEY)));

		mockMvc.perform(get("/api/v1/users/1/portfolio"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.availableCash", closeTo(627400.00, MONEY)))
			.andExpect(jsonPath("$.totalAccountValue", closeTo(904784.00, MONEY)));
	}

	@Test
	void sellWithoutSharesIsRejected() throws Exception {
		postOrder(1, """
				{"instrumentId":47,"side":"SELL","type":"MARKET","size":41}
				""").andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("REJECTED"));
	}

	@Test
	void sellOfTheShortSeedPositionIsRejected() throws Exception {
		postOrder(1, """
				{"instrumentId":31,"side":"SELL","type":"MARKET","size":1}
				""").andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("REJECTED"));
	}

	@Test
	void openLimitSellReservesShares() throws Exception {
		postOrder(1, """
				{"instrumentId":47,"side":"SELL","type":"LIMIT","size":40,"price":1000}
				""").andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("NEW"));

		postOrder(1, """
				{"instrumentId":47,"side":"SELL","type":"MARKET","size":1}
				""").andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("REJECTED"));
	}

	@Test
	void marketOrderWithoutAPriceIsRejected() throws Exception {
		postOrder(1, marketBuy(3, 5)).andExpect(status().isCreated())
			.andExpect(jsonPath("$.status").value("REJECTED"))
			.andExpect(jsonPath("$.size").value(5))
			.andExpect(jsonPath("$.price").value(nullValue()));

		postOrder(1, """
				{"instrumentId":3,"side":"BUY","type":"MARKET","amount":1000}
				""").andExpect(status().isCreated())
			.andExpect(jsonPath("$.status").value("REJECTED"))
			.andExpect(jsonPath("$.size").value(nullValue()))
			.andExpect(jsonPath("$.price").value(nullValue()));
	}

	@Test
	void ordersOnCurrencyAreBadRequests() throws Exception {
		postOrder(1, marketBuy(66, 1)).andExpect(status().isBadRequest());
	}

	@Test
	void cashTransfersAreBadRequests() throws Exception {
		postOrder(1, """
				{"instrumentId":47,"side":"CASH_IN","type":"MARKET","size":1}
				""").andExpect(status().isBadRequest());
	}

	@Test
	void unreadableBodiesAreReportedAsMalformed() throws Exception {
		postOrder(1, """
				{"instrumentId":47,"side":"HOLD","type":"MARKET","size":1}
				""").andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail").value("Malformed request body"));
		postOrder(1, """
				{"instrumentId":47,
				""").andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail").value("Malformed request body"));
	}

	@Test
	void malformedOrdersAreBadRequests() throws Exception {
		postOrder(1, """
				{"instrumentId":47,"side":"BUY","type":"MARKET","size":1,"amount":10}
				""").andExpect(status().isBadRequest());
		postOrder(1, """
				{"instrumentId":47,"side":"BUY","type":"LIMIT","size":1}
				""").andExpect(status().isBadRequest());
		postOrder(1, """
				{"instrumentId":47,"side":"BUY","type":"MARKET","size":0}
				""").andExpect(status().isBadRequest());
		postOrder(1, """
				{"instrumentId":47,"side":"BUY","type":"MARKET","size":1,"extra":true}
				""").andExpect(status().isBadRequest());
	}

}
