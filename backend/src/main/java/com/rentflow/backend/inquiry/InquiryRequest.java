package com.rentflow.backend.inquiry;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record InquiryRequest(
    @NotBlank(message = "Inquiry message is required")
    @Size(max = 1000, message = "Inquiry message must not exceed 1000 characters")
    String message
) {}
