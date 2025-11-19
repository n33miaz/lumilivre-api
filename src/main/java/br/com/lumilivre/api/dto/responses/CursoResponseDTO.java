package br.com.lumilivre.api.dto.responses;

import br.com.lumilivre.api.model.CursoModel;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CursoResponseDTO {

    private Integer id;
    private String nome;

    public CursoResponseDTO(CursoModel curso) {
        this.id = curso.getId();
        this.nome = curso.getNome();
    }
}