package com.oreo.insightfactory.summary;

import com.oreo.insightfactory.security.CurrentUser;
import com.oreo.insightfactory.summary.dto.SummaryRequestResponse;
import com.oreo.insightfactory.summary.dto.WeeklySummaryRequest;
import com.oreo.insightfactory.user.User;
import com.oreo.insightfactory.user.UserRole;
import jakarta.validation.ValidationException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
public class SummaryService {

    private final ApplicationEventPublisher eventPublisher;

    public SummaryService(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    public SummaryRequestResponse requestWeeklySummary(WeeklySummaryRequest request, boolean premium, boolean includeCharts, boolean attachPdf) {
        User user = requireUser();
        String targetBranch = request.branch();
        if (user.getRole() == UserRole.BRANCH) {
            targetBranch = user.getBranch();
        }
        if (user.getRole() == UserRole.CENTRAL && targetBranch == null) {
            targetBranch = request.branch();
        }
        if (targetBranch == null || targetBranch.isBlank()) {
            throw new ValidationException("Branch is required for summary requests");
        }
        if (user.getRole() == UserRole.BRANCH && !user.getBranch().equals(targetBranch)) {
            throw new ValidationException("Branch user cannot request reports for another branch");
        }

        LocalDate from = request.from();
        LocalDate to = request.to();
        if (from == null || to == null) {
            LocalDate now = LocalDate.now();
            to = now;
            from = now.minusDays(6);
        }
        if (to.isBefore(from)) {
            throw new ValidationException("Invalid date range");
        }
        String requestId = premium ? "req_premium_" + UUID.randomUUID() : "req_" + UUID.randomUUID();
        eventPublisher.publishEvent(new ReportRequestedEvent(
                requestId,
                user,
                from,
                to,
                targetBranch,
                request.emailTo(),
                premium,
                includeCharts,
                attachPdf
        ));
        String message = premium
                ? "Su reporte premium está siendo generado. Incluirá gráficos y PDF adjunto."
                : "Su solicitud de reporte está siendo procesada. Recibirá el resumen en " + request.emailTo() + " en unos momentos.";
        List<String> features = premium ? List.of("HTML_FORMAT", includeCharts ? "CHARTS" : null, attachPdf ? "PDF_ATTACHMENT" : null)
                .stream().filter(item -> item != null).toList() : List.of();
        return new SummaryRequestResponse(
                requestId,
                "PROCESSING",
                message,
                premium ? "60-90 segundos" : "30-60 segundos",
                OffsetDateTime.now(),
                features
        );
    }

    private User requireUser() {
        User user = CurrentUser.get();
        if (user == null) {
            throw new ValidationException("User not authenticated");
        }
        return user;
    }
}
