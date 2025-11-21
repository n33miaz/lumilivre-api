package br.com.lumilivre.api.dto.livro;

import java.time.LocalDate;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LivroRequest {

    private String isbn;
    private String nome;

    @JsonDeserialize(using = LocalDateDeserializer.class)
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate data_lancamento;

    private Integer numero_paginas;
    private String cdd;
    private String editora;
    private Integer numero_capitulos;
    private String classificacao_etaria;
    private String edicao;
    private Integer volume;
    private Integer quantidade;
    private String sinopse;
    private String tipo_capa;
    private String imagem;
    private String autor;
    private Set<String> generos;
}