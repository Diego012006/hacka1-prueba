package com.oreo.insightfactory.summary;

import java.math.BigDecimal;

public record SalesAggregates(
        long totalUnits,
        BigDecimal totalRevenue,
        String topSku,
        String topBranch
) {
    public static SalesAggregates empty() {
        return new SalesAggregates(0, BigDecimal.ZERO, null, null);
    }
}
