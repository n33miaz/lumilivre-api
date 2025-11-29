package br.com.lumilivre.api.controller.system;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import br.com.lumilivre.api.enums.Penalidade;
import br.com.lumilivre.api.enums.StatusEmprestimo;
import br.com.lumilivre.api.enums.StatusLivro;
import br.com.lumilivre.api.service.RelatorioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/relatorios")
@Tag(name = "13. Relatórios")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('ADMIN', 'BIBLIOTECARIO')")
public class RelatorioController {

    private final RelatorioService relatorioService;

    public RelatorioController(RelatorioService relatorioService) {
        this.relatorioService = relatorioService;
    }

    @GetMapping("/emprestimos")
    @Operation(summary = "Gera um relatório de empréstimos em PDF com filtros")
    public void relatorioEmprestimos(
            HttpServletResponse response,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
            @RequestParam(required = false) StatusEmprestimo status,
            @RequestParam(required = false) String matriculaAluno,
            @RequestParam(required = false) Integer idCurso,
            @RequestParam(required = false) String isbnOuTombo,
            @RequestParam(required = false) Integer idModulo)
            throws IOException {

        try {
            configurarRespostaPdf(response, "relatorio-emprestimos");
            relatorioService.gerarRelatorioEmprestimosPorFiltros(
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
            @Parameter(description = "Filtra por uma penalidade específica") @RequestParam(required = false) Penalidade penalidade,
            @Parameter(description = "Data de início da inclusão (YYYY-MM-DD)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @Parameter(description = "Data de fim da inclusão (YYYY-MM-DD)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim)
            throws IOException {
        configurarRespostaPdf(response, "relatorio-alunos");
        relatorioService.gerarRelatorioAlunosPorFiltros(response.getOutputStream(), idModulo, idCurso, idTurno,
                penalidade, dataInicio, dataFim);
    }

    @GetMapping("/livros")
    @Operation(summary = "Gera um relatório de livros em PDF com filtros")
    public void relatorioLivros(
            HttpServletResponse response,
            @RequestParam(required = false) String genero,
            @RequestParam(required = false) String autor,
            @RequestParam(required = false) String cdd,
            @RequestParam(required = false) String classificacaoEtaria,
            @RequestParam(required = false) String tipoCapa,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim)
            throws IOException {

        configurarRespostaPdf(response, "relatorio-livros");
        relatorioService.gerarRelatorioLivrosFiltrados(
                response.getOutputStream(),
                tratarString(genero),
                tratarString(autor),
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
        relatorioService.gerarRelatorioEstatisticasLivros(response.getOutputStream());
    }

    @GetMapping("/exemplares")
    public void relatorioExemplares(
            HttpServletResponse response,
            @RequestParam(required = false) StatusLivro status,
            @RequestParam(required = false) String isbnOuTombo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim)
            throws IOException {

        configurarRespostaPdf(response, "relatorio-exemplares");

        relatorioService.gerarRelatorioExemplaresFiltrados(
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
        relatorioService.gerarRelatorioCursosGeral(response.getOutputStream());
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