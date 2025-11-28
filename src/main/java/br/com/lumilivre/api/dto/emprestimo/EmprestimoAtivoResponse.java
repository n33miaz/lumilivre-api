package br.com.lumilivre.api.dto.emprestimo;

import br.com.lumilivre.api.enums.StatusEmprestimo;
import java.time.LocalDate;

public record EmprestimoAtivoResponse(

                Integer id,
                String livroNome,
                String alunoNome,
                String alunoMatricula,
                String tombo,
                LocalDate dataEmprestimo,
                LocalDate dataDevolucao,
                StatusEmprestimo statusEmprestimo) {
}