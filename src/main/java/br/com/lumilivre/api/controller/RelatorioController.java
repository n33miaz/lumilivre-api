package br.com.lumilivre.api.controller;

import br.com.lumilivre.api.enums.StatusEmprestimo;
import br.com.lumilivre.api.enums.StatusLivro;
import br.com.lumilivre.api.enums.Penalidade;
import br.com.lumilivre.api.service.RelatorioService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;

@RestController
@RequestMapping("/relatorios")
public class RelatorioController {

    private final RelatorioService relatorioService;

    public RelatorioController(RelatorioService relatorioService) {
        this.relatorioService = relatorioService;
    }

    // -----------------------
    // EMPRÉSTIMOS
    // -----------------------

    @GetMapping("/emprestimos")
    public void relatorioEmprestimos(
            HttpServletResponse response,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
            @RequestParam(required = false) StatusEmprestimo status,
            @RequestParam(required = false) Long idAluno,
            @RequestParam(required = false) Long idCurso,
            @RequestParam(required = false) String isbnOuTombo,
            @RequestParam(required = false) String modulo
    ) throws IOException {
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=relatorio-emprestimos.pdf");
        relatorioService.gerarRelatorioEmprestimosPorFiltros(response.getOutputStream(), dataInicio, dataFim, status, idAluno, idCurso, isbnOuTombo, modulo);
    }

    @GetMapping("/emprestimos/contagem-status")
    public void relatorioContagemEmprestimosPorStatus(HttpServletResponse response) throws IOException {
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=contagem-emprestimos-status.pdf");
        relatorioService.gerarContagemEmprestimosPorStatus(response.getOutputStream());
    }

    // -----------------------
    // ALUNOS
    // -----------------------

    @GetMapping("/alunos")
    public void relatorioAlunos(
            HttpServletResponse response,
            @RequestParam(required = false) String modulo,
            @RequestParam(required = false) Long idCurso,
            @RequestParam(required = false) String turno,
            @RequestParam(required = false) Penalidade penalidade
    ) throws IOException {
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=relatorio-alunos.pdf");
        relatorioService.gerarRelatorioAlunosPorFiltros(response.getOutputStream(), modulo, idCurso, turno, penalidade);
    }

    @GetMapping("/alunos/contagem-emprestimos")
    public void relatorioContagemEmprestimosPorAluno(
            HttpServletResponse response,
            @RequestParam(required = false) Integer topN
    ) throws IOException {
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=relatorio-alunos-contagem-emprestimos.pdf");
        relatorioService.gerarRelatorioContagemEmprestimosPorAluno(response.getOutputStream(), topN);
    }

    // -----------------------
    // LIVROS
    // -----------------------

    @GetMapping("/livros")
    public void relatorioLivros(
            HttpServletResponse response,
            @RequestParam(required = false) String genero,
            @RequestParam(required = false) String autor,
            @RequestParam(required = false) String cdd,
            @RequestParam(required = false) String classificacaoEtaria,
            @RequestParam(required = false) String tipoCapa
    ) throws IOException {
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=relatorio-livros.pdf");
        relatorioService.gerarRelatorioLivrosFiltrados(response.getOutputStream(), genero, autor, cdd, classificacaoEtaria, tipoCapa);
    }

    @GetMapping("/livros/estatisticas")
    public void relatorioEstatisticasLivros(HttpServletResponse response) throws IOException {
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=relatorio-livros-estatisticas.pdf");
        relatorioService.gerarRelatorioEstatisticasLivros(response.getOutputStream());
    }

    // -----------------------
    // EXEMPLARES
    // -----------------------

    @GetMapping("/exemplares")
    public void relatorioExemplares(
            HttpServletResponse response,
            @RequestParam(required = false) StatusLivro status,
            @RequestParam(required = false) String isbnOuTombo
    ) throws IOException {
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=relatorio-exemplares.pdf");
        relatorioService.gerarRelatorioExemplaresFiltrados(response.getOutputStream(), status, isbnOuTombo);
    }

    // -----------------------
    // CURSOS
    // -----------------------

    @GetMapping("/cursos/geral")
    public void relatorioCursosGeral(HttpServletResponse response) throws IOException {
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=relatorio-cursos-geral.pdf");
        relatorioService.gerarRelatorioCursosGeral(response.getOutputStream());
    }
}
