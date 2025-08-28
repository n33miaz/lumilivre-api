package br.com.lumilivre.api.data;

import br.com.lumilivre.api.enums.Turno;

public class ListaAutorDTO {
    private String codigo;
    private String nome;
    private String pseudonimo;
    private String nacionalidade;

    public ListaAutorDTO(String codigo, String nome, String pseudonimo, String nacionalidade) {
        this.codigo = codigo;
        this.nome = nome;
        this.pseudonimo = pseudonimo;
        this.nacionalidade = nacionalidade;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getPseudonimo() {
        return pseudonimo;
    }

    public void setPseudonimo(String pseudonimo) {
        this.pseudonimo = pseudonimo;
    }

    public String getNacionalidade() {
        return nacionalidade;
    }

    public void setNacionalidade(String nacionalidade) {
        this.nacionalidade = nacionalidade;
    }

}
