package com.oreo.insightfactory.common;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.OffsetDateTime;

public record ApiError(
        String error,
        String message,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
        OffsetDateTime timestamp,
        String path
) {
    public static ApiError of(String error, String message, String path) {
        return new ApiError(error, message, OffsetDateTime.now(), path);
    }
}
