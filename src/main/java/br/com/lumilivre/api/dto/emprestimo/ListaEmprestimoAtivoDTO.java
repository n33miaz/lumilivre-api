package br.com.lumilivre.api.dto.emprestimo;

import br.com.lumilivre.api.enums.StatusEmprestimo;
import java.time.LocalDate;

public record ListaEmprestimoAtivoDTO(
        Integer id,
        String livroNome,
        String alunoNome,
        String alunoMatricula,
        String tombo,
        LocalDate dataDevolucao,
        StatusEmprestimo statusEmprestimo) {
}