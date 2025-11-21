package br.com.lumilivre.api.dto.livro;

import java.time.LocalDate;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LivroRequest {

    @Size(max = 20, message = "O ISBN deve ter no máximo 20 caracteres")
    private String isbn;

    @NotBlank(message = "O nome do livro é obrigatório")
    @Size(max = 255, message = "O nome do livro deve ter no máximo 255 caracteres")
    private String nome;

    @NotNull(message = "A data de lançamento é obrigatória")
    @JsonDeserialize(using = LocalDateDeserializer.class)
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate data_lancamento;

    @NotNull(message = "O número de páginas é obrigatório")
    @Min(value = 1, message = "O livro deve ter pelo menos 1 página")
    private Integer numero_paginas;

    @NotBlank(message = "O código CDD é obrigatório")
    private String cdd;

    @NotBlank(message = "A editora é obrigatória")
    private String editora;

    private Integer numero_capitulos;

    @NotBlank(message = "A classificação etária é obrigatória")
    private String classificacao_etaria;

    private String edicao;
    private Integer volume;
    private Integer quantidade;
    private String sinopse;

    @NotBlank(message = "O tipo de capa é obrigatório")
    private String tipo_capa;

    private String imagem;

    @NotBlank(message = "O autor é obrigatório")
    private String autor;

    private Set<String> generos;
}