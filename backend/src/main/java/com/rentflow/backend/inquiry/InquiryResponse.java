package com.rentflow.backend.inquiry;

import java.time.Instant;

public record InquiryResponse(
    Long id,
    Long propertyId,
    String propertyTitle,
    Long tenantId,
    String tenantName,
    String tenantEmail,
    String tenantPhone,
    String message,
    InquiryStatus status,
    Instant createdAt
) {
    public static InquiryResponse from(Inquiry i) {
        return new InquiryResponse(
            i.getId(),
            i.getProperty().getId(),
            i.getProperty().getTitle(),
            i.getTenant().getId(),
            i.getTenant().getFullName(),
            i.getTenant().getEmail(),
            i.getTenant().getPhone(),
            i.getMessage(),
            i.getStatus(),
            i.getCreatedAt()
        );
    }
}
