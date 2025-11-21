package br.com.lumilivre.api.dto.livro;

import br.com.lumilivre.api.model.GeneroModel;
import br.com.lumilivre.api.model.LivroModel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Set;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LivroDetalheResponse {

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
    private Set<String> generos;
    private long exemplaresDisponiveis;
    private long totalExemplares;

    public LivroDetalheResponse(LivroModel livro, long exemplaresDisponiveis, long totalExemplares) {
        this.id = livro.getId();
        this.isbn = livro.getIsbn();
        this.nome = livro.getNome();
        this.autor = livro.getAutor();
        this.editora = livro.getEditora();
        this.dataLancamento = livro.getData_lancamento();
        this.numeroPaginas = livro.getNumero_paginas();
        this.sinopse = livro.getSinopse();
        this.imagem = livro.getImagem();

        this.cdd = livro.getCdd() != null ? livro.getCdd().getDescricao() : null;

        this.generos = livro.getGeneros().stream()
                .map(GeneroModel::getNome)
                .collect(Collectors.toSet());

        this.exemplaresDisponiveis = exemplaresDisponiveis;
        this.totalExemplares = totalExemplares;
    }
}