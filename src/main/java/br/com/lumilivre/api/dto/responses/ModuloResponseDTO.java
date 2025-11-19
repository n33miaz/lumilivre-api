package br.com.lumilivre.api.dto.responses;

import br.com.lumilivre.api.model.ModuloModel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ModuloResponseDTO {

    private Integer id;
    private String nome;

    public ModuloResponseDTO(ModuloModel model) {
        this.id = model.getId();
        this.nome = model.getNome();
    }
}