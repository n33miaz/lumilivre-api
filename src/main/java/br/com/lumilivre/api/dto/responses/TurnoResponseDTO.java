package br.com.lumilivre.api.dto.responses;

import br.com.lumilivre.api.model.TurnoModel;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TurnoResponseDTO {
    private Integer id;
    private String nome;

    public TurnoResponseDTO(TurnoModel model) {
        this.id = model.getId();
        this.nome = model.getNome();
    }
}