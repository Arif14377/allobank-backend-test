package com.allobank.backendtestallobank.dto.bill;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.allobank.backendtestallobank.dto.billgroup.SimpleUserResponse;

public record BillResponse(
		UUID id,
		SimpleUserResponse payer,
		BigDecimal amount,
		String description,
		List<BillDebtorResponse> debtors,
		Instant createdAt) {
}
