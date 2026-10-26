package com.rentflow.backend.property;

import java.math.BigDecimal;
import java.time.Instant;

public record PropertyResponse(
    Long id,
    String title,
    String description,
    BigDecimal rentAmount,
    String propertyType,
    String address,
    String city,
    int bedrooms,
    int bathrooms,
    boolean available,
    Long ownerId,
    String ownerName,
    Instant createdAt,
    Instant updatedAt
) {
    public static PropertyResponse from(Property p) {
        return new PropertyResponse(
            p.getId(), p.getTitle(), p.getDescription(),
            p.getRentAmount(), p.getPropertyType(), p.getAddress(),
            p.getCity(), p.getBedrooms(), p.getBathrooms(),
            p.isAvailable(), p.getOwner().getId(),
            p.getOwner().getFullName(), p.getCreatedAt(), p.getUpdatedAt()
        );
    }
}