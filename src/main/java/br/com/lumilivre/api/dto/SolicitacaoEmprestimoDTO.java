package br.com.lumilivre.api.dto;

import java.time.LocalDateTime;

import br.com.lumilivre.api.enums.StatusSolicitacao;

public class SolicitacaoEmprestimoDTO {

    private Integer id;
    private String alunoNome;
    private String alunoMatricula;
    private String exemplarTombo;
    private String livroNome;
    private LocalDateTime dataSolicitacao;
    private StatusSolicitacao status;
    private String observacao;

    public SolicitacaoEmprestimoDTO() {
    }

    public SolicitacaoEmprestimoDTO(Integer id, String alunoNome, String alunoMatricula,
            String exemplarTombo, String livroNome,
            LocalDateTime dataSolicitacao, StatusSolicitacao status,
            String observacao) {
        this.id = id;
        this.alunoNome = alunoNome;
        this.alunoMatricula = alunoMatricula;
        this.exemplarTombo = exemplarTombo;
        this.livroNome = livroNome;
        this.dataSolicitacao = dataSolicitacao;
        this.status = status;
        this.observacao = observacao;
    }

    // Getters e setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getAlunoNome() {
        return alunoNome;
    }

    public void setAlunoNome(String alunoNome) {
        this.alunoNome = alunoNome;
    }

    public String getAlunoMatricula() {
        return alunoMatricula;
    }

    public void setAlunoMatricula(String alunoMatricula) {
        this.alunoMatricula = alunoMatricula;
    }

    public String getExemplarTombo() {
        return exemplarTombo;
    }

    public void setExemplarTombo(String exemplarTombo) {
        this.exemplarTombo = exemplarTombo;
    }

    public String getLivroNome() {
        return livroNome;
    }

    public void setLivroNome(String livroNome) {
        this.livroNome = livroNome;
    }

    public LocalDateTime getDataSolicitacao() {
        return dataSolicitacao;
    }

    public void setDataSolicitacao(LocalDateTime dataSolicitacao) {
        this.dataSolicitacao = dataSolicitacao;
    }

    public StatusSolicitacao getStatus() {
        return status;
    }

    public void setStatus(StatusSolicitacao status) {
        this.status = status;
    }

    public String getObservacao() {
        return observacao;
    }

    public void setObservacao(String observacao) {
        this.observacao = observacao;
    }
}
