package br.com.lumilivre.api.dto.modulo;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModuloRequest {

    @NotBlank(message = "O nome do módulo é obrigatório")
    private String nome;
}