package br.com.lumilivre.api.controller;

import java.util.List;
import java.util.Locale;

import br.com.lumilivre.api.config.SwaggerTags;
import br.com.lumilivre.api.dto.dashboard.DashboardStatsResponse;
import br.com.lumilivre.api.dto.dashboard.LoansByMonthResponse;
import br.com.lumilivre.api.dto.dashboard.TopBookResponse;
import br.com.lumilivre.api.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController("v2DashboardController")
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Tag(name = SwaggerTags.DASHBOARD)
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/stats")
    @Operation(operationId = "dashboard.stats")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public ResponseEntity<DashboardStatsResponse> stats(Locale locale) {
        return ResponseEntity.ok()
                .header("Content-Language", locale.toLanguageTag())
                .body(dashboardService.getStats());
    }

    @GetMapping("/top-books")
    @Operation(operationId = "dashboard.topBooks")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public ResponseEntity<List<TopBookResponse>> topBooks(Locale locale) {
        return ResponseEntity.ok()
                .header("Content-Language", locale.toLanguageTag())
                .body(dashboardService.getTopBooks());
    }

    @GetMapping("/loans-by-month")
    @Operation(operationId = "dashboard.loansByMonth")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public ResponseEntity<List<LoansByMonthResponse>> loansByMonth(Locale locale) {
        return ResponseEntity.ok()
                .header("Content-Language", locale.toLanguageTag())
                .body(dashboardService.getLoansByMonth());
    }
}
