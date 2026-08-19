package com.allobank.backendtestallobank.billgroup.dto;

import java.util.List;

public record BillGroupListResponse(
		List<BillGroupSummaryResponse> data,
		int total) {
}
