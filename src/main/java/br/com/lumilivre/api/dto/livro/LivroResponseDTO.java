package br.com.lumilivre.api.dto.livro;

import br.com.lumilivre.api.model.LivroModel;
import br.com.lumilivre.api.model.GeneroModel;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@Setter
public class LivroResponseDTO {

    private Long id;
    private String isbn;
    private String nome;
    private String autor;
    private String editora;
    private LocalDate dataLancamento;
    private Integer numeroPaginas;
    private String sinopse;
    private String imagem;
    private String cdd;
    private String classificacaoEtaria;
    private String tipoCapa;
    private Integer edicao;
    private Integer volume;
    private Integer quantidade;
    private Set<String> generos;

    public LivroResponseDTO(LivroModel livro) {
        this.id = livro.getId();
        this.isbn = livro.getIsbn();
        this.nome = livro.getNome();
        this.autor = livro.getAutor();
        this.editora = livro.getEditora();
        this.dataLancamento = livro.getData_lancamento();
        this.numeroPaginas = livro.getNumero_paginas();
        this.sinopse = livro.getSinopse();
        this.imagem = livro.getImagem();

        this.cdd = (livro.getCdd() != null) ? livro.getCdd().getCodigo() + " - " + livro.getCdd().getDescricao() : null;

        this.classificacaoEtaria = (livro.getClassificacao_etaria() != null)
                ? livro.getClassificacao_etaria().getStatus()
                : null;
        this.tipoCapa = (livro.getTipo_capa() != null) ? livro.getTipo_capa().getStatus() : null;

        this.edicao = livro.getEdicao() != null ? Integer.parseInt(livro.getEdicao().replaceAll("\\D", "")) : null;
        this.volume = livro.getVolume();
        this.quantidade = livro.getQuantidade();

        this.generos = livro.getGeneros().stream()
                .map(GeneroModel::getNome)
                .collect(Collectors.toSet());
    }
}