package com.allobank.backendtestallobank.dto.billgroup;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record BillGroupResponse(
		UUID id,
		String name,
		SimpleUserResponse createdBy,
		List<MemberResponse> members,
		Instant createdAt) {
}
