package br.com.lumilivre.api.dto.v1.turno;

import br.com.lumilivre.api.model.StudyShift;
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

    public TurnoResponse(StudyShift model) {
        this.id = model.getId();
        this.nome = model.getName();
    }
}