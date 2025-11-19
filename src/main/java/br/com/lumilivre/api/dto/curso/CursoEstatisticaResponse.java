package br.com.lumilivre.api.dto.curso;

public class CursoEstatisticaResponse {

    private final String nomeCurso;
    private final long quantidadeAlunos;
    private final long totalEmprestimos;

    public CursoEstatisticaResponse(String nomeCurso, long quantidadeAlunos, Long totalEmprestimos) {
        this.nomeCurso = nomeCurso;
        this.quantidadeAlunos = quantidadeAlunos;
        this.totalEmprestimos = (totalEmprestimos == null) ? 0L : totalEmprestimos;
    }

    public String getNomeCurso() {
        return nomeCurso;
    }

    public long getQuantidadeAlunos() {
        return quantidadeAlunos;
    }

    public long getTotalEmprestimos() {
        return totalEmprestimos;
    }

    public double getMediaEmprestimosPorAluno() {
        if (quantidadeAlunos == 0) {
            return 0.0;
        }
        return (double) totalEmprestimos / quantidadeAlunos;
    }
}