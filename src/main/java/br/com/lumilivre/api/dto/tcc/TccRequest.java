package br.com.lumilivre.api.dto.tcc;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TccRequest {

    private String titulo;
    private String alunos;
    private String orientadores;

    @JsonProperty("curso_id")
    private Integer cursoId;

    private String anoConclusao;
    private String semestreConclusao;
    private String linkExterno;

    @Builder.Default
    private Boolean ativo = true;
}