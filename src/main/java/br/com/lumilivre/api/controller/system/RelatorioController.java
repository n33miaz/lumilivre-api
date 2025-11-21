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
            @Parameter(description = "Data de início do período (YYYY-MM-DD)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @Parameter(description = "Data de fim do período (YYYY-MM-DD)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
            @Parameter(description = "Filtra por um status de empréstimo específico") @RequestParam(required = false) StatusEmprestimo status,
            @Parameter(description = "Filtra pela matrícula exata do aluno") @RequestParam(required = false) String matriculaAluno,
            @Parameter(description = "Filtra por ID do curso") @RequestParam(required = false) Integer idCurso,
            @Parameter(description = "Filtra por ISBN do livro ou Tombo do exemplar") @RequestParam(required = false) String isbnOuTombo,
            @Parameter(description = "Filtra por ID do módulo") @RequestParam(required = false) Integer idModulo)
            throws IOException {
        configurarRespostaPdf(response, "relatorio-emprestimos");
        relatorioService.gerarRelatorioEmprestimosPorFiltros(response.getOutputStream(), dataInicio, dataFim, status,
                matriculaAluno, idCurso, isbnOuTombo, idModulo);
    }

    @GetMapping("/alunos")
    @Operation(summary = "Gera um relatório de alunos em PDF com filtros")
    public void relatorioAlunos(
            HttpServletResponse response,
            @Parameter(description = "Filtra por ID do módulo") @RequestParam(required = false) Integer idModulo,
            @Parameter(description = "Filtra por ID do curso") @RequestParam(required = false) Integer idCurso,
            @Parameter(description = "Filtra por ID do turno") @RequestParam(required = false) Integer idTurno,
            @Parameter(description = "Filtra por uma penalidade específica") @RequestParam(required = false) Penalidade penalidade)
            throws IOException {
        configurarRespostaPdf(response, "relatorio-alunos");
        relatorioService.gerarRelatorioAlunosPorFiltros(response.getOutputStream(), idModulo, idCurso, idTurno,
                penalidade);
    }

    @GetMapping("/livros")
    @Operation(summary = "Gera um relatório de livros em PDF com filtros")
    public void relatorioLivros(
            HttpServletResponse response,
            @Parameter(description = "Filtra por nome do gênero") @RequestParam(required = false) String genero,
            @Parameter(description = "Filtra por nome parcial do autor") @RequestParam(required = false) String autor,
            @Parameter(description = "Filtra por código CDD") @RequestParam(required = false) String cdd,
            @Parameter(description = "Filtra por classificação etária") @RequestParam(required = false) String classificacaoEtaria,
            @Parameter(description = "Filtra por tipo de capa") @RequestParam(required = false) String tipoCapa)
            throws IOException {
        configurarRespostaPdf(response, "relatorio-livros");
        relatorioService.gerarRelatorioLivrosFiltrados(response.getOutputStream(), genero, autor, cdd,
                classificacaoEtaria, tipoCapa);
    }

    @GetMapping("/livros/estatisticas")
    @Operation(summary = "Gera um relatório estatístico sobre os livros")
    public void relatorioEstatisticasLivros(HttpServletResponse response) throws IOException {
        configurarRespostaPdf(response, "estatisticas-livros");
        relatorioService.gerarRelatorioEstatisticasLivros(response.getOutputStream());
    }

    @GetMapping("/exemplares")
    @Operation(summary = "Gera um relatório de exemplares em PDF com filtros")
    public void relatorioExemplares(
            HttpServletResponse response,
            @Parameter(description = "Filtra por um status de exemplar específico") @RequestParam(required = false) StatusLivro status,
            @Parameter(description = "Filtra por parte do ISBN do livro ou do Tombo do exemplar") @RequestParam(required = false) String isbnOuTombo)
            throws IOException {
        configurarRespostaPdf(response, "relatorio-exemplares");
        relatorioService.gerarRelatorioExemplaresFiltrados(response.getOutputStream(), status, isbnOuTombo);
    }

    @GetMapping("/cursos/geral")
    @Operation(summary = "Gera um relatório geral sobre os cursos")
    public void relatorioCursosGeral(HttpServletResponse response) throws IOException {
        configurarRespostaPdf(response, "relatorio-geral-cursos");
        relatorioService.gerarRelatorioCursosGeral(response.getOutputStream());
    }

    private void configurarRespostaPdf(HttpServletResponse response, String nomeBase) {
        response.setContentType("application/pdf");
        String dataAtual = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String nomeArquivo = String.format("%s_%s.pdf", nomeBase, dataAtual);
        response.setHeader("Content-Disposition", "attachment; filename=\"" + nomeArquivo + "\"");
    }
}