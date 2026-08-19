package com.allobank.backendtestallobank.bill.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateBillRequest(
		@NotNull
		UUID payerId,

		@NotNull
		@Positive
		BigDecimal amount,

		@Size(max = 255)
		String description,

		@NotEmpty
		List<@Valid @NotNull BillDebtorRequest> debtors) {
}
