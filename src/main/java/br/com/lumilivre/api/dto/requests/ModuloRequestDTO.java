package br.com.lumilivre.api.dto.requests;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ModuloRequestDTO {
    @NotBlank(message = "O nome do módulo é obrigatório")
    private String nome;
}