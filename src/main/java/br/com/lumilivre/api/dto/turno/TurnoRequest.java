package br.com.lumilivre.api.dto.turno;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TurnoRequest {
    
    @NotBlank(message = "O nome do turno é obrigatório")
    private String nome;
}