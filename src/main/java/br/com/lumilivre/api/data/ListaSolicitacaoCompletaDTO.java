package br.com.lumilivre.api.data;

import java.time.LocalDateTime;

import br.com.lumilivre.api.enums.StatusSolicitacao;

public class ListaSolicitacaoCompletaDTO {
    private Integer id;
    private String alunoNome;
    private String alunoMatricula;
    private String exemplarTombo;
    private String livroNome;
    private LocalDateTime dataSolicitacao;
    private StatusSolicitacao status;
    private String observacao;

    public ListaSolicitacaoCompletaDTO(Integer id, String alunoNome, String alunoMatricula,
            String exemplarTombo, String livroNome, LocalDateTime dataSolicitacao,
            StatusSolicitacao status, String observacao) {
        this.id = id;
        this.alunoNome = alunoNome;
        this.alunoMatricula = alunoMatricula;
        this.exemplarTombo = exemplarTombo;
        this.livroNome = livroNome;
        this.dataSolicitacao = dataSolicitacao;
        this.status = status;
        this.observacao = observacao;
    }

    public Integer getId() {
        return id;
    }

    public String getAlunoNome() {
        return alunoNome;
    }

    public String getAlunoMatricula() {
        return alunoMatricula;
    }

    public String getExemplarTombo() {
        return exemplarTombo;
    }

    public String getLivroNome() {
        return livroNome;
    }

    public LocalDateTime getDataSolicitacao() {
        return dataSolicitacao;
    }

    public StatusSolicitacao getStatus() {
        return status;
    }

    public String getObservacao() {
        return observacao;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public void setAlunoNome(String alunoNome) {
        this.alunoNome = alunoNome;
    }

    public void setAlunoMatricula(String alunoMatricula) {
        this.alunoMatricula = alunoMatricula;
    }

    public void setExemplarTombo(String exemplarTombo) {
        this.exemplarTombo = exemplarTombo;
    }

    public void setLivroNome(String livroNome) {
        this.livroNome = livroNome;
    }

    public void setDataSolicitacao(LocalDateTime dataSolicitacao) {
        this.dataSolicitacao = dataSolicitacao;
    }

    public void setStatus(StatusSolicitacao status) {
        this.status = status;
    }

    public void setObservacao(String observacao) {
        this.observacao = observacao;
    }
}
