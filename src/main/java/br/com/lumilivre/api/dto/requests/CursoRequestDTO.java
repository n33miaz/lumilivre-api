package br.com.lumilivre.api.dto.requests;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CursoRequestDTO {

    @NotBlank(message = "O nome do curso é obrigatório")
    private String nome;
}