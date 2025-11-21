package br.com.lumilivre.api.dto.turno;

import br.com.lumilivre.api.model.TurnoModel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TurnoResponse {

    private Integer id;
    private String nome;

    public TurnoResponse(TurnoModel model) {
        this.id = model.getId();
        this.nome = model.getNome();
    }
}