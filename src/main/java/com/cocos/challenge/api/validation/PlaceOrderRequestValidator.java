package com.cocos.challenge.api.validation;

import com.cocos.challenge.api.dto.request.PlaceOrderRequest;
import com.cocos.challenge.domain.enums.OrderType;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PlaceOrderRequestValidator implements ConstraintValidator<ValidPlaceOrder, PlaceOrderRequest> {

	@Override
	public boolean isValid(PlaceOrderRequest value, ConstraintValidatorContext context) {
		if (value == null) {
			return true;
		}
		boolean valid = true;
		context.disableDefaultConstraintViolation();
		boolean hasSize = value.size() != null;
		boolean hasAmount = value.amount() != null;
		if (hasSize == hasAmount) {
			context.buildConstraintViolationWithTemplate("Provide exactly one of size or amount")
				.addConstraintViolation();
			valid = false;
		}
		if (value.type() == OrderType.LIMIT && value.price() == null) {
			context.buildConstraintViolationWithTemplate("Price is required for LIMIT orders")
				.addPropertyNode("price")
				.addConstraintViolation();
			valid = false;
		}
		return valid;
	}

}
