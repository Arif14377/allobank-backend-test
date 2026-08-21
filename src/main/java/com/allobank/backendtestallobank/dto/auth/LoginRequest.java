package com.allobank.backendtestallobank.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
		@NotBlank
		String fullName,

		@NotBlank
		@Email
		String email) {
}