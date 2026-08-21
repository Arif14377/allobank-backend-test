package com.allobank.backendtestallobank.dto.billgroup;

import java.util.List;

public record BillGroupListResponse(
		List<BillGroupSummaryResponse> data,
		int total) {
}
