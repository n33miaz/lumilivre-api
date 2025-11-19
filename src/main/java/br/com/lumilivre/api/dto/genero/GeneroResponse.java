package br.com.lumilivre.api.dto.genero;

import br.com.lumilivre.api.model.GeneroModel;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GeneroResponse {

    private Integer id;
    private String nome;

    public GeneroResponse() {
    }

    public GeneroResponse(GeneroModel model) {
        this.id = model.getId();
        this.nome = model.getNome();
    }

}