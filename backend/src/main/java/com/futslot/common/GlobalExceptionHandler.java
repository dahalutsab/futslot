package com.futslot.common;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Renders every error as RFC 9457 problem details with an extra {@code code} property.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

	private final Words words;

	GlobalExceptionHandler(Words words) {
		this.words = words;
	}

	/** {@code detail} is in the request's language (Accept-Language), so apps can show it as it is. */
	@ExceptionHandler(ApiException.class)
	ProblemDetail handleApiException(ApiException ex) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(ex.getStatus(), this.words.forRequest().error(ex));
		problem.setProperty("code", ex.getCode());
		return problem;
	}

	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		Map<String, String> errors = new LinkedHashMap<>();
		for (FieldError error : ex.getBindingResult().getFieldErrors()) {
			errors.putIfAbsent(error.getField(), error.getDefaultMessage());
		}
		ProblemDetail problem = ex.getBody();
		problem.setDetail(this.words.forRequest().orElse("error.VALIDATION_FAILED", "Request validation failed"));
		problem.setProperty("code", "VALIDATION_FAILED");
		problem.setProperty("errors", errors);
		return handleExceptionInternal(ex, problem, headers, status, request);
	}

	@Override
	protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(status,
				"Request body is missing or malformed: " + rootMessage(ex));
		problem.setProperty("code", "MALFORMED_REQUEST");
		return handleExceptionInternal(ex, problem, headers, status, request);
	}

	private static String rootMessage(Throwable ex) {
		Throwable cause = ex;
		while (cause.getCause() != null) {
			cause = cause.getCause();
		}
		String message = (cause.getMessage() != null) ? cause.getMessage() : cause.getClass().getSimpleName();
		// Jackson messages end with a source-location dump after a newline; the first line is enough
		return message.lines().findFirst().orElse(message);
	}

}
