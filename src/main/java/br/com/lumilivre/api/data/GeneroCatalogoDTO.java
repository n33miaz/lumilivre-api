package br.com.lumilivre.api.data;

import java.util.List;

public class GeneroCatalogoDTO {
    private String nome;
    private List<LivroResponseMobileGeneroDTO> livros;

    public GeneroCatalogoDTO(String nome, List<LivroResponseMobileGeneroDTO> livros) {
        this.nome = nome;
        this.livros = livros;
    }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    
    public List<LivroResponseMobileGeneroDTO> getLivros() { return livros; }
    public void setLivros(List<LivroResponseMobileGeneroDTO> livros) { this.livros = livros; }
}