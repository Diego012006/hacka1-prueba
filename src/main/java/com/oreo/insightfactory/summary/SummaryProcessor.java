package com.oreo.insightfactory.summary;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.ByteArrayOutputStream;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class SummaryProcessor {

    private static final Logger log = LoggerFactory.getLogger(SummaryProcessor.class);

    private final SalesAggregationService aggregationService;
    private final JavaMailSender mailSender;
    private final RestClient restClient;
    private final String token;
    private final String modelId;

    public SummaryProcessor(SalesAggregationService aggregationService,
                            JavaMailSender mailSender,
                            RestClient.Builder restClientBuilder,
                            @Value("${GITHUB_MODELS_URL:https://models.github.com/v1/chat/completions}") String endpoint,
                            @Value("${GITHUB_TOKEN:}") String token,
                            @Value("${MODEL_ID:gpt-5-mini}") String modelId) {
        this.aggregationService = aggregationService;
        this.mailSender = mailSender;
        this.restClient = restClientBuilder.baseUrl(endpoint).build();
        this.token = token;
        this.modelId = modelId;
    }

    @Async
    @org.springframework.context.event.EventListener
    public void onReportRequested(ReportRequestedEvent event) {
        try {
            process(event);
        } catch (Exception ex) {
            log.error("Failed to process summary {}", event.requestId(), ex);
        }
    }

    private void process(ReportRequestedEvent event) throws MessagingException {
        OffsetDateTime from = event.from().atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime to = event.to().atTime(23, 59, 59).atOffset(ZoneOffset.UTC);
        SalesAggregates aggregates = aggregationService.calculateAggregates(from, to, event.branch());

        String summaryText = buildSummary(event, aggregates);
        if (event.premium()) {
            sendPremiumEmail(event, aggregates, summaryText);
        } else {
            sendPlainEmail(event, aggregates, summaryText);
        }
    }

    private String buildSummary(ReportRequestedEvent event, SalesAggregates aggregates) {
        if (token == null || token.isBlank()) {
            return fallbackSummary(aggregates);
        }
        try {
            Map<String, Object> payload = Map.of(
                    "model", modelId,
                    "messages", List.of(
                            Map.of("role", "system", "content", "Eres un analista que escribe resúmenes breves y claros para emails corporativos."),
                            Map.of("role", "user", "content", promptForAggregates(event, aggregates))
                    ),
                    "max_tokens", 200
            );
            LlmResponse response = restClient.post()
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(LlmResponse.class);
            if (response != null && !response.choices().isEmpty()) {
                return response.choices().get(0).message().content();
            }
        } catch (Exception ex) {
            log.warn("LLM request failed, using fallback summary", ex);
        }
        return fallbackSummary(aggregates);
    }

    private String promptForAggregates(ReportRequestedEvent event, SalesAggregates aggregates) {
        return "Con estos datos: totalUnits=" + aggregates.totalUnits()
                + ", totalRevenue=" + aggregates.totalRevenue()
                + ", topSku=" + aggregates.topSku()
                + ", topBranch=" + aggregates.topBranch()
                + ". Devuelve un resumen ≤120 palabras para enviar por email.";
    }

    private String fallbackSummary(SalesAggregates aggregates) {
        if (aggregates.totalUnits() == 0) {
            return "No se registraron ventas en el periodo solicitado.";
        }
        return String.format("Se vendieron %d unidades con un total de %s USD. SKU destacado: %s. Sucursal líder: %s.",
                aggregates.totalUnits(),
                aggregates.totalRevenue(),
                aggregates.topSku(),
                aggregates.topBranch());
    }

    private void sendPlainEmail(ReportRequestedEvent event, SalesAggregates aggregates, String summaryText) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(event.emailTo());
        message.setSubject("Reporte Semanal Oreo - " + event.from() + " a " + event.to());
        message.setText(summaryText + System.lineSeparator() +
                buildStatsBlock(aggregates));
        mailSender.send(message);
    }

    private String buildStatsBlock(SalesAggregates aggregates) {
        return "Total unidades: " + aggregates.totalUnits()
                + " | Total ingresos: $" + aggregates.totalRevenue()
                + " | SKU top: " + aggregates.topSku()
                + " | Sucursal top: " + aggregates.topBranch();
    }

    private void sendPremiumEmail(ReportRequestedEvent event, SalesAggregates aggregates, String summaryText) throws MessagingException {
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);
        helper.setTo(event.emailTo());
        helper.setSubject("Reporte Premium Oreo - " + event.from() + " a " + event.to());
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><body>");
        html.append("<h1 style=\"color:#6B46C1;\">🍪 Reporte Semanal Oreo</h1>");
        html.append("<p>").append(summaryText).append("</p>");
        html.append("<div><strong>Total unidades:</strong> ").append(aggregates.totalUnits()).append("</div>");
        html.append("<div><strong>Total ingresos:</strong> $").append(aggregates.totalRevenue()).append("</div>");
        html.append("<div><strong>SKU top:</strong> ").append(aggregates.topSku()).append("</div>");
        html.append("<div><strong>Sucursal top:</strong> ").append(aggregates.topBranch()).append("</div>");
        if (event.includeCharts()) {
            String chartUrl = buildQuickChartUrl(aggregates);
            html.append("<div><img alt=\"Gráfico de ventas\" src=\"").append(chartUrl).append("\"/></div>");
        }
        html.append("</body></html>");
        helper.setText(html.toString(), true);

        if (event.attachPdf()) {
            helper.addAttachment("reporte.pdf", () -> buildPdf(summaryText, aggregates), "application/pdf");
        }

        mailSender.send(mimeMessage);
    }

    private java.io.InputStream buildPdf(String summaryText, SalesAggregates aggregates) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            Document document = new Document();
            PdfWriter.getInstance(document, outputStream);
            document.open();
            document.add(new Paragraph("Reporte Semanal Oreo"));
            document.add(new Paragraph(summaryText));
            document.add(new Paragraph("Total unidades: " + aggregates.totalUnits()));
            document.add(new Paragraph("Total ingresos: $" + aggregates.totalRevenue()));
            document.add(new Paragraph("SKU top: " + aggregates.topSku()));
            document.add(new Paragraph("Sucursal top: " + aggregates.topBranch()));
            document.close();
            return new java.io.ByteArrayInputStream(outputStream.toByteArray());
        } catch (DocumentException ex) {
            throw new RuntimeException("Failed to generate PDF", ex);
        }
    }

    private String buildQuickChartUrl(SalesAggregates aggregates) {
        return "https://quickchart.io/chart?c={type:'bar',data:{labels:['Unidades','Ingresos'],datasets:[{label:'Resumen',data:["
                + aggregates.totalUnits() + "," + aggregates.totalRevenue() + "]}]}}";
    }

    public record LlmResponse(List<Choice> choices) {
        public record Choice(Message message) {
        }
        public record Message(String content) {
        }
    }
}
