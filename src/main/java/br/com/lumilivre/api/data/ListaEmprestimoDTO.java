package br.com.lumilivre.api.data;

import java.time.LocalDateTime;

import br.com.lumilivre.api.enums.StatusEmprestimo;

public class ListaEmprestimoDTO {
    private StatusEmprestimo statusEmprestimo;
    private String livroNome;
    private String nomeAluno;
    private LocalDateTime dataEmprestimo;
    private LocalDateTime dataDevolucao;

    public ListaEmprestimoDTO(StatusEmprestimo statusEmprestimo, String livroNome, String nomeAluno,
            LocalDateTime dataEmprestimo, LocalDateTime dataDevolucao) {
        this.statusEmprestimo = statusEmprestimo;
        this.livroNome = livroNome;
        this.nomeAluno = nomeAluno;
        this.dataEmprestimo = dataEmprestimo;
        this.dataDevolucao = dataDevolucao;
    }

    public StatusEmprestimo getStatusEmprestimo() {
        return statusEmprestimo;
    }

    public void setStatusEmprestimo(StatusEmprestimo statusEmprestimo) {
        this.statusEmprestimo = statusEmprestimo;
    }

    public String getlivroNome() {
        return livroNome;
    }

    public void setlivroNome(String livroNome) {
        this.livroNome = livroNome;
    }

    public String getNomeAluno() {
        return nomeAluno;
    }

    public void setNomeAluno(String nomeAluno) {
        this.nomeAluno = nomeAluno;
    }

    public LocalDateTime getDataEmprestimo() {
        return dataEmprestimo;
    }

    public void setDataEmprestimo(LocalDateTime dataEmprestimo) {
        this.dataEmprestimo = dataEmprestimo;
    }

    public LocalDateTime getDataDevolucao() {
        return dataDevolucao;
    }

    public void setDataDevolucao(LocalDateTime dataDevolucao) {
        this.dataDevolucao = dataDevolucao;
    }

}
