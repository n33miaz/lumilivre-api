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
    private String tipoCapa;
    private String classificacaoEtaria;
    private String cddCodigo;
    private String tipoCapaRaw;
    private String classificacaoEtariaRaw;
    private String edicao;
    private Integer volume;
    private Set<String> generos;
    private long exemplaresDisponiveis;
    private long totalExemplares;
    private Double avaliacao;

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
        this.edicao = livro.getEdicao();
        this.volume = livro.getVolume();

        if (livro.getCdd() != null) {
            this.cdd = livro.getCdd().getDescricao();
            this.cddCodigo = livro.getCdd().getCodigo();
        }

        if (livro.getTipo_capa() != null) {
            this.tipoCapa = livro.getTipo_capa().getStatus();
            this.tipoCapaRaw = livro.getTipo_capa().name();
        }

        if (livro.getClassificacao_etaria() != null) {
            this.classificacaoEtaria = livro.getClassificacao_etaria().getStatus();
            this.classificacaoEtariaRaw = livro.getClassificacao_etaria().name();
        }

        this.generos = livro.getGeneros().stream()
                .map(GeneroModel::getNome)
                .collect(Collectors.toSet());

        this.exemplaresDisponiveis = exemplaresDisponiveis;
        this.totalExemplares = totalExemplares;
        this.avaliacao = livro.getAvaliacao();
    }
}