package com.allobank.backendtestallobank.common.error;

import java.time.Instant;
import java.util.List;

public record ApiErrorResponse(
		Instant timestamp,
		int status,
		String error,
		String message,
		String path,
		List<FieldErrorResponse> fieldErrors) {

	public static ApiErrorResponse of(int status, String error, String message, String path) {
		return new ApiErrorResponse(Instant.now(), status, error, message, path, List.of());
	}
}
