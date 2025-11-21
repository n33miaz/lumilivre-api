package br.com.lumilivre.api.dto.turno;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TurnoRequest {

    @NotBlank(message = "O nome do turno é obrigatório")
    private String nome;
}