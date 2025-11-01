package com.oreo.insightfactory.summary.web;

import com.oreo.insightfactory.summary.SummaryService;
import com.oreo.insightfactory.summary.dto.SummaryRequestResponse;
import com.oreo.insightfactory.summary.dto.WeeklySummaryRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/sales/summary")
public class SummaryController {

    private final SummaryService summaryService;

    public SummaryController(SummaryService summaryService) {
        this.summaryService = summaryService;
    }

    @PostMapping("/weekly")
    public ResponseEntity<SummaryRequestResponse> requestWeekly(@RequestBody @Valid WeeklySummaryRequest request) {
        SummaryRequestResponse response = summaryService.requestWeeklySummary(request, false, false, false);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @PostMapping("/weekly/premium")
    public ResponseEntity<SummaryRequestResponse> requestWeeklyPremium(@RequestBody @Valid WeeklySummaryRequest request) {
        SummaryRequestResponse response = summaryService.requestWeeklySummary(request, true, true, true);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }
}
