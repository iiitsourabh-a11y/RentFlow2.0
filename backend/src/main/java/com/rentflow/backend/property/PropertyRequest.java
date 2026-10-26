package com.rentflow.backend.property;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record PropertyRequest(
    @NotBlank @Size(max = 150) String title,
    String description,
    @NotNull @DecimalMin(value = "0.01") @Digits(integer = 10, fraction = 2)
    BigDecimal rentAmount,
    @NotBlank @Size(max = 30) String propertyType,
    @NotBlank @Size(max = 255) String address,
    @NotBlank @Size(max = 100) String city,
    @Min(0) @Max(100) int bedrooms,
    @Min(0) @Max(100) int bathrooms,
    Boolean available
) {}