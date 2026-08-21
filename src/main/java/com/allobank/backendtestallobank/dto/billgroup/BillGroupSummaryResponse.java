package com.allobank.backendtestallobank.dto.billgroup;

import java.time.Instant;
import java.util.UUID;

public record BillGroupSummaryResponse(
		UUID id,
		String name,
		SimpleUserResponse createdBy,
		long memberCount,
		Instant createdAt) {
}
