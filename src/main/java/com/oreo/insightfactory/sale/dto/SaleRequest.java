package com.oreo.insightfactory.sale.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record SaleRequest(
        @NotBlank String sku,
        @Min(1) int units,
        @NotNull BigDecimal price,
        @NotBlank String branch,
        @NotNull OffsetDateTime soldAt
) {
}
