package com.rentflow.backend.inquiry;

import jakarta.validation.constraints.NotNull;

public record InquiryStatusUpdateRequest(
    @NotNull(message = "Status is required")
    InquiryStatus status
) {}
