package br.com.lumilivre.api.data;

// DTO ESPECÍFICO para a lista administrativa
public class ListaLivroDTO {
    private String nome;
    private String isbn;
    private String autor;
    private String editora;
    private Integer quantidadeExemplares;

    public ListaLivroDTO(String nome, String isbn, String autor, String editora, Integer quantidadeExemplares) {
        this.nome = nome;
        this.isbn = isbn;
        this.autor = autor;
        this.editora = editora;
        this.quantidadeExemplares = quantidadeExemplares;
    }

    public String getNome() {
        return nome;
    }

    public String getIsbn() {
        return isbn;
    }

    public String getAutor() {
        return autor;
    }

    public String getEditora() {
        return editora;
    }

    public Integer getQuantidadeExemplares() {
        return quantidadeExemplares;
    }
}