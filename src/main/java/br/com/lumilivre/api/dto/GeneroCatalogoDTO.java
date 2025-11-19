package br.com.lumilivre.api.dto;

import java.util.List;

import br.com.lumilivre.api.dto.livro.LivroMobileResponse;

public class GeneroCatalogoDTO {
    private String nome;
    private List<LivroMobileResponse> livros;

    public GeneroCatalogoDTO(String nome, List<LivroMobileResponse> livros) {
        this.nome = nome;
        this.livros = livros;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public List<LivroMobileResponse> getLivros() {
        return livros;
    }

    public void setLivros(List<LivroMobileResponse> livros) {
        this.livros = livros;
    }
}