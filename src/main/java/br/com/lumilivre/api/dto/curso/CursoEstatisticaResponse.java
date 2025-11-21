package br.com.lumilivre.api.dto.curso;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CursoEstatisticaResponse {

    private String nomeCurso;
    private long quantidadeAlunos;
    private long totalEmprestimos;

    public double getMediaEmprestimosPorAluno() {
        if (quantidadeAlunos == 0) {
            return 0.0;
        }
        return (double) totalEmprestimos / quantidadeAlunos;
    }

    public CursoEstatisticaResponse(String nomeCurso, long quantidadeAlunos, Long totalEmprestimos) {
        this.nomeCurso = nomeCurso;
        this.quantidadeAlunos = quantidadeAlunos;
        this.totalEmprestimos = (totalEmprestimos == null) ? 0L : totalEmprestimos;
    }
}