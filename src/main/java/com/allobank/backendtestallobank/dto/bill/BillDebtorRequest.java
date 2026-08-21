package com.allobank.backendtestallobank.dto.bill;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record BillDebtorRequest(
		@NotNull
		UUID userId,

		@NotNull
		@Positive
		BigDecimal amount) {
}
