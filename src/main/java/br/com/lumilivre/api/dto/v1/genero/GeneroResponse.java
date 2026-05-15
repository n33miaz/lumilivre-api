package br.com.lumilivre.api.dto.v1.genero;

import br.com.lumilivre.api.model.Genre;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GeneroResponse {

    private Integer id;
    private String nome;

    public GeneroResponse(Genre model) {
        this.id = model.getId();
        this.nome = model.getName();
    }
}