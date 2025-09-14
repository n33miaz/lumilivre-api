package br.com.lumilivre.api.data;

public class LivroResponseMobileGeneroDTO {
    
    private String imagem;
    private String titulo;
    private String autor;
    
    public LivroResponseMobileGeneroDTO(String imagem, String titulo, String autor) {
        this.imagem = imagem;
        this.titulo = titulo;
        this.autor = autor;
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
