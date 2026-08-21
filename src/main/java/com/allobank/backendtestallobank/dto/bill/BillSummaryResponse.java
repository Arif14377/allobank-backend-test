package com.allobank.backendtestallobank.dto.bill;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.allobank.backendtestallobank.dto.billgroup.SimpleUserResponse;

public record BillSummaryResponse(
        UUID id,
        SimpleUserResponse payer,
        BigDecimal amount,
        String description,
        Instant createdAt) {
}
