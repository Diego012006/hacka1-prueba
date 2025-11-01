package com.oreo.insightfactory.summary.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

public record WeeklySummaryRequest(
        LocalDate from,
        LocalDate to,
        String branch,
        @NotBlank @Email String emailTo
) {
}
