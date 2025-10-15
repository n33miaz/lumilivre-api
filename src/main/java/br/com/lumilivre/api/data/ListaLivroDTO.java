package br.com.lumilivre.api.data;

import br.com.lumilivre.api.enums.Cdd;
import br.com.lumilivre.api.enums.StatusLivro;

public class ListaLivroDTO {

    private StatusLivro status;
    private String tomboExemplar;
    private String isbn;
    private Cdd cdd;
    private String nome;
    private String genero;
    private String autor;
    private String editora;
    private String localizacao_fisica;

    public ListaLivroDTO(StatusLivro status, String tomboExemplar, String isbn, Cdd cdd, String nome,
            String genero, String autor, String editora, String localizacao_fisica) {
        this.status = status;
        this.tomboExemplar = tomboExemplar;
        this.isbn = isbn;
        this.cdd = cdd;
        this.nome = nome;
        this.genero = genero;
        this.autor = autor;
        this.editora = editora;
        this.localizacao_fisica = localizacao_fisica;
    }

    public StatusLivro getStatus() {
        return status;
    }
    public void setStatus(StatusLivro status) {
        this.status = status;
    }

    public String getTomboExemplar() {
        return tomboExemplar;
    }
    public void setTomboExemplar(String tomboExemplar) {
        this.tomboExemplar = tomboExemplar;
    }

    public String getIsbn() {
        return isbn;
    }
    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

    public Cdd getCdd() {
        return cdd;
    }
    public void setCdd(Cdd cdd) {
        this.cdd = cdd;
    }

    public String getNome() {
        return nome;
    }
    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getGenero() {
        return genero;
    }
    public void setGenero(String genero) {
        this.genero = genero;
    }

    public String getAutor() {
        return autor;
    }
    public void setAutor(String autor) {
        this.autor = autor;
    }

    public String getEditora() {
        return editora;
    }
    public void setEditora(String editora) {
        this.editora = editora;
    }

    public String getLocalizacao_fisica() {
        return localizacao_fisica;
    }
    public void setLocalizacao_fisica(String localizacao_fisica) {
        this.localizacao_fisica = localizacao_fisica;
    }
}
