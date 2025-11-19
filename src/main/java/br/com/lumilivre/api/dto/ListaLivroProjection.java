package br.com.lumilivre.api.dto;

public interface ListaLivroProjection {
    String getStatus();

    String getTomboExemplar();

    String getIsbn();

    String getCdd();

    String getNome();

    String getGenero();

    String getAutor();

    String getEditora();

    String getLocalizacao_fisica();
}