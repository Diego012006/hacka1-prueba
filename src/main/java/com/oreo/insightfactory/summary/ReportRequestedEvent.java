package com.oreo.insightfactory.summary;

import com.oreo.insightfactory.user.User;
import java.time.LocalDate;

public record ReportRequestedEvent(
        String requestId,
        User requester,
        LocalDate from,
        LocalDate to,
        String branch,
        String emailTo,
        boolean premium,
        boolean includeCharts,
        boolean attachPdf
) {
}
