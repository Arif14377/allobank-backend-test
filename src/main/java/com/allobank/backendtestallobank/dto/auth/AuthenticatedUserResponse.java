package com.allobank.backendtestallobank.dto.auth;

import java.util.UUID;

public record AuthenticatedUserResponse(
		UUID id,
		String fullName,
		String email) {
}