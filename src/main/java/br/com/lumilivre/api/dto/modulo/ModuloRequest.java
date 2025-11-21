package br.com.lumilivre.api.dto.modulo;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ModuloRequest {
    
    @NotBlank(message = "O nome do módulo é obrigatório")
    private String nome;
}