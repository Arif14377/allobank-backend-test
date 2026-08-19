package com.allobank.backendtestallobank.bill.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.allobank.backendtestallobank.billgroup.dto.SimpleUserResponse;

public record BillSummaryResponse(
        UUID id,
        SimpleUserResponse payer,
        BigDecimal amount,
        String description,
        Instant createdAt) {
}
