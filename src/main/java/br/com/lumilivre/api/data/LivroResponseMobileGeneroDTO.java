package br.com.lumilivre.api.data;

public class LivroResponseMobileGeneroDTO {
    private Long id;
    private String imagem;
    private String titulo;
    private String autor;

    public LivroResponseMobileGeneroDTO(Long id, String imagem, String titulo, String autor) {
        this.id = id;
        this.imagem = imagem;
        this.titulo = titulo;
        this.autor = autor;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getImagem() {
        return imagem;
    }

    public void setImagem(String imagem) {
        this.imagem = imagem;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getAutor() {
        return autor;
    }

    public void setAutor(String autor) {
        this.autor = autor;
    }
}
