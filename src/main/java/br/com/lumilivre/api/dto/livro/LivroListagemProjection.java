package br.com.lumilivre.api.dto.livro;

public interface LivroListagemProjection {
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