package br.com.lumilivre.api.dto.v1.tcc;

import br.com.lumilivre.api.model.Thesis;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ThesisResponse {

    private UUID id;
    private String titulo;
    private String alunos;
    private String orientadores;
    private String curso;
    private String anoConclusao;
    private String semestreConclusao;
    private String arquivoPdf;
    private String foto;
    private String linkExterno;
    private Boolean ativo;

    public ThesisResponse(Thesis thesis) {
        this.id = thesis.getId();
        this.titulo = thesis.getTitle();
        this.alunos = thesis.getAuthors();
        this.orientadores = thesis.getAdvisors();
        this.curso = thesis.getCourse() != null ? thesis.getCourse().getName() : null;
        this.anoConclusao = thesis.getCompletionYear() != null ? thesis.getCompletionYear().toString() : null;
        this.semestreConclusao = thesis.getCompletionSemester();
        this.arquivoPdf = thesis.getPdfUrl();
        this.foto = thesis.getCoverUrl();
        this.linkExterno = thesis.getExternalUrl();
        this.ativo = thesis.getActive();
    }
}
