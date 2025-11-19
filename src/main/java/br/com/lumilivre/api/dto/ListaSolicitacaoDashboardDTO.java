package br.com.lumilivre.api.dto;

import java.time.LocalDateTime;

public class ListaSolicitacaoDashboardDTO {

    private String alunoNome;
    private String livroNome;
    private String tombo;
    private LocalDateTime dataSolicitacao;

    public ListaSolicitacaoDashboardDTO(String alunoNome, String livroNome, String tombo,
            LocalDateTime dataSolicitacao) {
        this.alunoNome = alunoNome;
        this.livroNome = livroNome;
        this.tombo = tombo;
        this.dataSolicitacao = dataSolicitacao;
    }

    public String getAlunoNome() {
        return alunoNome;
    }

    public String getLivroNome() {
        return livroNome;
    }

    public String getTombo() {
        return tombo;
    }

    public LocalDateTime getDataSolicitacao() {
        return dataSolicitacao;
    }

    public void setAlunoNome(String alunoNome) {
        this.alunoNome = alunoNome;
    }

    public void setLivroNome(String livroNome) {
        this.livroNome = livroNome;
    }

    public void setTombo(String tombo) {
        this.tombo = tombo;
    }

    public void setDataSolicitacao(LocalDateTime dataSolicitacao) {
        this.dataSolicitacao = dataSolicitacao;
    }
}