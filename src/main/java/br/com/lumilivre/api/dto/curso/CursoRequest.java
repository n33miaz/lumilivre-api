package br.com.lumilivre.api.dto.curso;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CursoRequest {

    @NotBlank(message = "O nome do curso é obrigatório")
    private String nome;
}