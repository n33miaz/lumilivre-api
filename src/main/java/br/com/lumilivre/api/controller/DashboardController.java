package br.com.lumilivre.api.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.lumilivre.api.dto.dashboard.DashboardStatsResponse;
import br.com.lumilivre.api.dto.dashboard.EmprestimosPorMesResponse;
import br.com.lumilivre.api.dto.dashboard.TopLivroResponse;
import br.com.lumilivre.api.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Indicadores gerenciais consolidados (materialized views)")
@SecurityRequirement(name = "bearerAuth")
public class DashboardController {

    private final DashboardService dashboardService;

    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @GetMapping("/stats")
    @Operation(summary = "Retorna contadores consolidados (ativos, atrasados, pendentes, reservas)")
    public ResponseEntity<DashboardStatsResponse> stats() {
        return ResponseEntity.ok(dashboardService.getStats());
    }

    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @GetMapping("/top-livros")
    @Operation(summary = "Top 20 livros mais emprestados")
    public ResponseEntity<List<TopLivroResponse>> topLivros() {
        return ResponseEntity.ok(dashboardService.getTopLivros());
    }

    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @GetMapping("/emprestimos-por-mes")
    @Operation(summary = "Volume de empréstimos por mês (últimos 12 meses)")
    public ResponseEntity<List<EmprestimosPorMesResponse>> emprestimosPorMes() {
        return ResponseEntity.ok(dashboardService.getEmprestimosPorMes());
    }
}
