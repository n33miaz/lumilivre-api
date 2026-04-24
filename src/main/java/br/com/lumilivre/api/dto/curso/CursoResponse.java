package br.com.lumilivre.api.dto.curso;

import br.com.lumilivre.api.model.Course;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CursoResponse {

    private Integer id;
    private String nome;

    public CursoResponse(Course curso) {
        this.id = curso.getId();
        this.nome = curso.getName();
    }
}