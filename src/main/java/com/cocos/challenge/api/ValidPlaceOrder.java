package com.cocos.challenge.api;

import com.cocos.challenge.domain.OrderType;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidPlaceOrder.Validator.class)
public @interface ValidPlaceOrder {

	String message() default "Invalid order";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};

	class Validator implements ConstraintValidator<ValidPlaceOrder, PlaceOrderRequest> {

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
}
