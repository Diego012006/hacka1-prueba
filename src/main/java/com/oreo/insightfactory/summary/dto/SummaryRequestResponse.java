package com.oreo.insightfactory.summary.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record SummaryRequestResponse(
        String requestId,
        String status,
        String message,
        String estimatedTime,
        OffsetDateTime requestedAt,
        List<String> features
) {
    public SummaryRequestResponse(String requestId, String status, String message, String estimatedTime, OffsetDateTime requestedAt) {
        this(requestId, status, message, estimatedTime, requestedAt, List.of());
    }
}
