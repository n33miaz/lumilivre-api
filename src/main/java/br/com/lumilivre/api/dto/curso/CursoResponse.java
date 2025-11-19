package br.com.lumilivre.api.dto.curso;

import br.com.lumilivre.api.model.CursoModel;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CursoResponse {

    private Integer id;
    private String nome;

    public CursoResponse(CursoModel curso) {
        this.id = curso.getId();
        this.nome = curso.getNome();
    }
}