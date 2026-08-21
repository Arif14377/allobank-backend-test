package com.allobank.backendtestallobank.dto.bill;

import java.math.BigDecimal;
import java.util.UUID;

public record BillDebtorResponse(
		UUID userId,
		String fullName,
		BigDecimal amount) {
}
