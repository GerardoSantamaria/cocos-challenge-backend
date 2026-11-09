package com.cocos.challenge.infrastructure.persistence;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "marketdata")
public class MarketDataEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@Column(name = "instrumentid")
	private Integer instrumentId;

	@Column(name = "high", precision = 10, scale = 2)
	private BigDecimal highPrice;

	@Column(name = "low", precision = 10, scale = 2)
	private BigDecimal lowPrice;

	@Column(name = "open", precision = 10, scale = 2)
	private BigDecimal openPrice;

	@Column(name = "close", precision = 10, scale = 2)
	private BigDecimal closePrice;

	@Column(name = "previousclose", precision = 10, scale = 2)
	private BigDecimal previousClose;

	@Column(name = "date")
	private LocalDate marketDate;

	public Integer getId() {
		return id;
	}

	public Integer getInstrumentId() {
		return instrumentId;
	}

	public BigDecimal getClosePrice() {
		return closePrice;
	}

	public BigDecimal getPreviousClose() {
		return previousClose;
	}

	public LocalDate getMarketDate() {
		return marketDate;
	}
}
