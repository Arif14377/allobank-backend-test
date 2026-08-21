package com.allobank.backendtestallobank.dto.bill;

import java.util.List;

public record BillListResponse(
        List<BillSummaryResponse> data,
        int total) {
}
