package br.com.lumilivre.api.data;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import br.com.lumilivre.api.enums.Penalidade;

public class AlunoDTO {

    private String matricula;
    private String nomeCompleto;
    private String cpf;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy")
    private LocalDate data_nascimento;
    private String celular;
    private String email;
    private Integer cursoId;
    private String cep;
    private String logradouro;
    private String complemento;
    private String localidade;
    private String bairro;
    private String uf;
    private Integer numero_casa;
    private Penalidade penalidade;
    private LocalDateTime penalidadeExpiraEm;
    private Integer emprestimosCount = 0;

    public Integer getCursoId() {
        return cursoId;
    }

    public void setCursoId(Integer cursoId) {
        this.cursoId = cursoId;
    }

    public String getMatricula() {
        return matricula;
    }

    public void setMatricula(String matricula) {
        this.matricula = matricula;
    }

    public String getNomeCompleto() {
        return nomeCompleto;
    }

    public void setNomeCompleto(String nomeCompleto) {
        this.nomeCompleto = nomeCompleto;
    }

    public String getCpf() {
        return cpf;
    }

    public void setCpf(String cpf) {
        this.cpf = cpf;
    }

    public LocalDate getDataNascimento() {
        return data_nascimento;
    }

    public void setDataNascimento(LocalDate data_nascimento) {
        this.data_nascimento = data_nascimento;
    }

    public String getCelular() {
        return celular;
    }

    public void setCelular(String celular) {
        this.celular = celular;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCep() {
        return cep;
    }

    public void setCep(String cep) {
        this.cep = cep;
    }

    public String getLogradouro() {
        return logradouro;
    }

    public void setLogradouro(String logradouro) {
        this.logradouro = logradouro;
    }

    public String getComplemento() {
        return complemento;
    }

    public void setComplemento(String complemento) {
        this.complemento = complemento;
    }

    public String getLocalidade() {
        return localidade;
    }

    public void setLocalidade(String localidade) {
        this.localidade = localidade;
    }

    public String getBairro() {
        return bairro;
    }

    public void setBairro(String bairro) {
        this.bairro = bairro;
    }

    public String getUf() {
        return uf;
    }

    public void setUf(String uf) {
        this.uf = uf;
    }

    public Integer getNumero_casa() {
        return numero_casa;
    }

    public void setNumero_casa(Integer numero_casa) {
        this.numero_casa = numero_casa;
    }

    public LocalDate getData_nascimento() {
        return data_nascimento;
    }

    public void setData_nascimento(LocalDate data_nascimento) {
        this.data_nascimento = data_nascimento;
    }

    public Penalidade getPenalidade() {
        return penalidade;
    }

    public void setPenalidade(Penalidade penalidade) {
        this.penalidade = penalidade;
    }

    public LocalDateTime getPenalidadeExpiraEm() {
        return penalidadeExpiraEm;
    }

    public void setPenalidadeExpiraEm(LocalDateTime penalidadeExpiraEm) {
        this.penalidadeExpiraEm = penalidadeExpiraEm;
    }

    public Integer getEmprestimosCount() {
        return emprestimosCount;
    }

    public void setEmprestimosCount(Integer emprestimosCount) {
        this.emprestimosCount = emprestimosCount;
    }

}
