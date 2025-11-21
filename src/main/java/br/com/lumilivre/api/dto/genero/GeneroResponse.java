package br.com.lumilivre.api.dto.genero;

import br.com.lumilivre.api.model.GeneroModel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GeneroResponse {

    private Integer id;
    private String nome;

    public GeneroResponse(GeneroModel model) {
        this.id = model.getId();
        this.nome = model.getNome();
    }
}