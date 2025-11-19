package br.com.lumilivre.api.dto.curso;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CursoRequest {

    @NotBlank(message = "O nome do curso é obrigatório")
    private String nome;
}