package br.com.lumilivre.api.controller;

import br.com.lumilivre.api.enums.StatusEmprestimo;
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

    @GetMapping("/emprestimos")
    public void relatorioEmprestimos(
            HttpServletResponse response,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
            @RequestParam(required = false) StatusEmprestimo status
    ) throws IOException {
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=relatorio-emprestimos.pdf");
        relatorioService.gerarRelatorioEmprestimos(response.getOutputStream(), dataInicio, dataFim, status);
    }

    @GetMapping("/alunos")
    public void relatorioAlunos(HttpServletResponse response) throws IOException {
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=relatorio-alunos.pdf");
        relatorioService.gerarRelatorioAlunos(response.getOutputStream());
    }

    @GetMapping("/livros")
    public void relatorioLivros(HttpServletResponse response) throws IOException {
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=relatorio-livros.pdf");
        relatorioService.gerarRelatorioLivros(response.getOutputStream());
    }

    @GetMapping("/exemplares")
    public void relatorioExemplares(HttpServletResponse response) throws IOException {
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=relatorio-exemplares.pdf");
        relatorioService.gerarRelatorioExemplares(response.getOutputStream());
    }
}
