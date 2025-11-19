package br.com.lumilivre.api.dto.requests;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TurnoRequestDTO {
    @NotBlank(message = "O nome do turno é obrigatório")
    private String nome;
}