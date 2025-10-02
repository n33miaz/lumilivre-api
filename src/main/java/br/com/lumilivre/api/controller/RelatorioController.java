package br.com.lumilivre.api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.lumilivre.api.model.EmprestimoModel;
import br.com.lumilivre.api.service.RelatorioService;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/relatorios")
public class RelatorioController {


    private final RelatorioService relatorioService;

    public RelatorioController(RelatorioService relatorioService) {
        this.relatorioService = relatorioService;
    }
    
    @GetMapping("/emprestimos")
    public void relatorioEmprestimos(HttpServletResponse response) throws IOException {
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=relatorio-emprestimos.pdf");
        relatorioService.gerarRelatorioEmprestimos(response.getOutputStream());
    }
    
    @GetMapping("/emprestimos-atrasados")
    public void relatorioEmprestimosAtrasados(HttpServletResponse response) throws IOException {
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=relatorio-emprestimos-atrasados.pdf");
        relatorioService.gerarRelatorioEmprestimosAtivosEAtrasados(response.getOutputStream());
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
}

