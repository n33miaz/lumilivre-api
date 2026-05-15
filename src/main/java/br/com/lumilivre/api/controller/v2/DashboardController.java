package br.com.lumilivre.api.controller.v2;

import java.util.List;
import java.util.Locale;

import br.com.lumilivre.api.dto.dashboard.DashboardStatsV2Response;
import br.com.lumilivre.api.dto.dashboard.LoansByMonthResponse;
import br.com.lumilivre.api.dto.dashboard.TopBookResponse;
import br.com.lumilivre.api.mapper.v2.DashboardMapper;
import br.com.lumilivre.api.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController("v2DashboardController")
@RequestMapping("/api/v2/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;
    private final DashboardMapper mapper;

    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public ResponseEntity<DashboardStatsV2Response> stats(Locale locale) {
        return ResponseEntity.ok()
                .header("Content-Language", locale.toLanguageTag())
                .body(mapper.toV2Stats(dashboardService.getStats()));
    }

    @GetMapping("/top-books")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public ResponseEntity<List<TopBookResponse>> topBooks(Locale locale) {
        return ResponseEntity.ok()
                .header("Content-Language", locale.toLanguageTag())
                .body(mapper.toTopBooks(dashboardService.getTopLivros()));
    }

    @GetMapping("/loans-by-month")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public ResponseEntity<List<LoansByMonthResponse>> loansByMonth(Locale locale) {
        return ResponseEntity.ok()
                .header("Content-Language", locale.toLanguageTag())
                .body(mapper.toLoansByMonthList(dashboardService.getEmprestimosPorMes()));
    }
}
