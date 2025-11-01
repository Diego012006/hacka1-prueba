package com.oreo.insightfactory.sale.dto;

import com.oreo.insightfactory.sale.Sale;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record SaleResponse(
        UUID id,
        String sku,
        int units,
        BigDecimal price,
        String branch,
        OffsetDateTime soldAt,
        String createdBy
) {
    public static SaleResponse from(Sale sale) {
        return new SaleResponse(
                sale.getId(),
                sale.getSku(),
                sale.getUnits(),
                sale.getPrice(),
                sale.getBranch(),
                sale.getSoldAt(),
                sale.getCreatedBy() != null ? sale.getCreatedBy().getUsername() : null
        );
    }
}
