package br.com.lumilivre.api.dto.solicitacao;

import java.time.LocalDateTime;

public class SolicitacaoResumoResponse {

    private String alunoNome;
    private String livroNome;
    private LocalDateTime dataSolicitacao;

    public SolicitacaoResumoResponse(String alunoNome, String livroNome, LocalDateTime dataSolicitacao) {
        this.alunoNome = alunoNome;
        this.livroNome = livroNome;
        this.dataSolicitacao = dataSolicitacao;
    }

    public String getAlunoNome() {
        return alunoNome;
    }

    public String getLivroNome() {
        return livroNome;
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

    public void setDataSolicitacao(LocalDateTime dataSolicitacao) {
        this.dataSolicitacao = dataSolicitacao;
    }
}
