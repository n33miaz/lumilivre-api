package br.com.lumilivre.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TccRequestDTO {
    private String titulo;
    private String alunos;
    private String orientadores;

    @JsonProperty("curso_id")
    private Integer cursoId;

    private String anoConclusao;
    private String semestreConclusao;
    private String linkExterno;
    private Boolean ativo = true;

}