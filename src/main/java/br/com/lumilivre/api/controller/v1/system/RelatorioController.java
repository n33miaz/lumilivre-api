package br.com.lumilivre.api.controller.v1.system;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import br.com.lumilivre.api.enums.BookCopyStatus;
import br.com.lumilivre.api.enums.LoanStatus;
import br.com.lumilivre.api.enums.PenaltyCode;
import br.com.lumilivre.api.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/relatorios")
@Tag(name = "14. Relatórios")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
public class RelatorioController {

    private final ReportService reportService;

    public RelatorioController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/emprestimos")
    @Operation(summary = "Gera um relatório de empréstimos em PDF com filtros")
    public void relatorioEmprestimos(
            HttpServletResponse response,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
            @RequestParam(required = false) LoanStatus status,
            @RequestParam(required = false) String matriculaAluno,
            @RequestParam(required = false) Integer idCurso,
            @RequestParam(required = false) String isbnOuTombo,
            @RequestParam(required = false) Integer idModulo)
            throws IOException {

        try {
            configurarRespostaPdf(response, "relatorio-emprestimos");
            reportService.gerarRelatorioEmprestimosPorFiltros(
                    response.getOutputStream(),
                    dataInicio,
                    dataFim,
                    status,
                    tratarString(matriculaAluno),
                    idCurso,
                    tratarString(isbnOuTombo),
                    idModulo);
        } catch (Exception e) {
            System.err.println("ERRO AO GERAR RELATÓRIO DE EMPRÉSTIMOS:");
            e.printStackTrace();
            throw e;
        }
    }

    @GetMapping("/alunos")
    @Operation(summary = "Gera um relatório de alunos em PDF com filtros")
    public void relatorioAlunos(
            HttpServletResponse response,
            @Parameter(description = "Filtra por ID do módulo") @RequestParam(required = false) Integer idModulo,
            @Parameter(description = "Filtra por ID do curso") @RequestParam(required = false) Integer idCurso,
            @Parameter(description = "Filtra por ID do turno") @RequestParam(required = false) Integer idTurno,
            @Parameter(description = "Filtra por uma penalidade específica") @RequestParam(required = false) PenaltyCode penalidade,
            @Parameter(description = "Data de início da inclusão (YYYY-MM-DD)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @Parameter(description = "Data de fim da inclusão (YYYY-MM-DD)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim)
            throws IOException {
        configurarRespostaPdf(response, "relatorio-alunos");
        reportService.gerarRelatorioAlunosPorFiltros(response.getOutputStream(), idModulo, idCurso, idTurno,
                penalidade, dataInicio, dataFim);
    }

    @GetMapping("/livros")
    @Operation(summary = "Gera um relatório de livros em PDF com filtros")
    public void relatorioLivros(
            HttpServletResponse response,
            @RequestParam(required = false) String genero,
            @RequestParam(required = false) String autor,
            @RequestParam(required = false) String editora,
            @RequestParam(required = false) String cdd,
            @RequestParam(required = false) String classificacaoEtaria,
            @RequestParam(required = false) String tipoCapa,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim)
            throws IOException {

        configurarRespostaPdf(response, "relatorio-livros");
        reportService.gerarRelatorioLivrosFiltrados(
                response.getOutputStream(),
                tratarString(genero),
                tratarString(autor),
                tratarString(editora),
                tratarString(cdd),
                tratarString(classificacaoEtaria),
                tratarString(tipoCapa),
                dataInicio,
                dataFim);
    }

    @GetMapping("/livros/estatisticas")
    @Operation(summary = "Gera um relatório estatístico sobre os livros")
    public void relatorioEstatisticasLivros(HttpServletResponse response) throws IOException {
        configurarRespostaPdf(response, "estatisticas-livros");
        reportService.gerarRelatorioEstatisticasLivros(response.getOutputStream());
    }

    @GetMapping("/exemplares")
    @Operation(summary = "Gera um relatório de exemplares em PDF com filtros")
    public void relatorioExemplares(
            HttpServletResponse response,
            @RequestParam(required = false) BookCopyStatus status,
            @RequestParam(required = false) String isbnOuTombo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim)
            throws IOException {

        configurarRespostaPdf(response, "relatorio-exemplares");

        reportService.gerarRelatorioExemplaresFiltrados(
                response.getOutputStream(),
                status,
                tratarString(isbnOuTombo),
                dataInicio,
                dataFim);
    }

    @GetMapping("/cursos/geral")
    @Operation(summary = "Gera um relatório geral sobre os cursos")
    public void relatorioCursosGeral(HttpServletResponse response) throws IOException {
        configurarRespostaPdf(response, "relatorio-geral-cursos");
        reportService.gerarRelatorioCursosGeral(response.getOutputStream());
    }

    private String tratarString(String valor) {
        if (valor != null && valor.trim().isEmpty()) {
            return null;
        }
        return valor;
    }

    private void configurarRespostaPdf(HttpServletResponse response, String nomeBase) {
        response.setContentType("application/pdf");
        String dataAtual = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String nomeArquivo = String.format("%s_%s.pdf", nomeBase, dataAtual);
        response.setHeader("Content-Disposition", "attachment; filename=\"" + nomeArquivo + "\"");
    }
}
