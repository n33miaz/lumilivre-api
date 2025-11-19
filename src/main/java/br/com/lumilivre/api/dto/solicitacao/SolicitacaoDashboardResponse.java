package br.com.lumilivre.api.dto.solicitacao;

import java.time.LocalDateTime;

public class SolicitacaoDashboardResponse {

    private String alunoNome;
    private String livroNome;
    private String tombo;
    private LocalDateTime dataSolicitacao;

    public SolicitacaoDashboardResponse(String alunoNome, String livroNome, String tombo,
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