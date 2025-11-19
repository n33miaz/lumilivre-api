package br.com.lumilivre.api.dto;

import br.com.lumilivre.api.model.GeneroModel;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GeneroDTO {

    private Integer id;
    private String nome;

    public GeneroDTO() {
    }

    public GeneroDTO(GeneroModel model) {
        this.id = model.getId();
        this.nome = model.getNome();
    }

}