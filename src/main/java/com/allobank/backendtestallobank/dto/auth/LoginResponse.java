package com.allobank.backendtestallobank.dto.auth;

import java.time.Instant;

public record LoginResponse(
		String accessToken,
		String tokenType,
		Instant expiresAt,
		AuthenticatedUserResponse user) {
}