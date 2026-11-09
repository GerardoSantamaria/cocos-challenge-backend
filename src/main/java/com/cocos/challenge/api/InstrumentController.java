package com.cocos.challenge.api;

import java.util.List;

import com.cocos.challenge.application.InstrumentSearchService;
import com.cocos.challenge.domain.InstrumentMatch;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/instruments")
@Validated
public class InstrumentController {

	private final InstrumentSearchService instrumentSearchService;

	public InstrumentController(InstrumentSearchService instrumentSearchService) {
		this.instrumentSearchService = instrumentSearchService;
	}

	@GetMapping("/search")
	public List<InstrumentMatch> search(@RequestParam @NotBlank @Size(max = 50) String q) {
		return instrumentSearchService.search(q);
	}
}
