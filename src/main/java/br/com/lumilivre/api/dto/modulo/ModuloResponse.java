package br.com.lumilivre.api.dto.modulo;

import br.com.lumilivre.api.model.AcademicModule;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ModuloResponse {

    private Integer id;
    private String nome;

    public ModuloResponse(AcademicModule model) {
        this.id = model.getId();
        this.nome = model.getName();
    }
}