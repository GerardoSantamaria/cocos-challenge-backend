package com.cocos.challenge.infrastructure.persistence.entity;

import com.cocos.challenge.domain.enums.InstrumentType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "instruments")
public class InstrumentEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@Column(name = "ticker", length = 10)
	private String ticker;

	@Column(name = "name", length = 255)
	private String name;

	@Enumerated(EnumType.STRING)
	@Column(name = "type", length = 10)
	private InstrumentType type;

	public Integer getId() {
		return id;
	}

	public String getTicker() {
		return ticker;
	}

	public String getName() {
		return name;
	}

	public InstrumentType getType() {
		return type;
	}

}
