package com.cocos.challenge.api;

import com.cocos.challenge.domain.exception.ConflictException;
import com.cocos.challenge.domain.exception.InvalidOrderException;
import com.cocos.challenge.domain.exception.NotFoundException;

import jakarta.validation.ConstraintViolationException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

	@ExceptionHandler(NotFoundException.class)
	ProblemDetail notFound(NotFoundException ex) {
		return problem(HttpStatus.NOT_FOUND, "Not Found", ex.getMessage());
	}

	@ExceptionHandler(InvalidOrderException.class)
	ProblemDetail invalid(InvalidOrderException ex) {
		return problem(HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage());
	}

	@ExceptionHandler(ConflictException.class)
	ProblemDetail conflict(ConflictException ex) {
		return problem(HttpStatus.CONFLICT, "Conflict", ex.getMessage());
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	ProblemDetail unreadable(HttpMessageNotReadableException ex) {
		return problem(HttpStatus.BAD_REQUEST, "Bad Request", "Malformed request body");
	}

	@ExceptionHandler(ConstraintViolationException.class)
	ProblemDetail constraintViolation(ConstraintViolationException ex) {
		return problem(HttpStatus.BAD_REQUEST, "Bad Request", "Request validation failed");
	}

	private static ProblemDetail problem(HttpStatus status, String title, String detail) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
		problem.setTitle(title);
		return problem;
	}
}
