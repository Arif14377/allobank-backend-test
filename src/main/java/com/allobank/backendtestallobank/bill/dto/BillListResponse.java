package com.allobank.backendtestallobank.bill.dto;

import java.util.List;

public record BillListResponse(
        List<BillSummaryResponse> data,
        int total) {
}
