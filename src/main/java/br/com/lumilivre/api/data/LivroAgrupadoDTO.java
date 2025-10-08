package br.com.lumilivre.api.data;

public class LivroAgrupadoDTO {
    private String isbn;
    private String nome;
    private String autor;
    private String editora;
    private Long quantidade; 

    public LivroAgrupadoDTO(String isbn, String nome, String autor, String editora, Long quantidade) {
        this.isbn = isbn;
        this.nome = nome;
        this.autor = autor;
        this.editora = editora;
        this.quantidade = quantidade;
    }

    public String getIsbn() { return isbn; }
    public void setIsbn(String isbn) { this.isbn = isbn; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getAutor() { return autor; }
    public void setAutor(String autor) { this.autor = autor; }

    public String getEditora() { return editora; }
    public void setEditora(String editora) { this.editora = editora; }
    
    public Long getQuantidade() { return quantidade; }
    public void setQuantidade(Long quantidade) { this.quantidade = quantidade; }
}