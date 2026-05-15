package br.com.lumilivre.api.dto.v1.livro;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LivroMobileResponse {

    private UUID id;
    private String imagem;
    private String titulo;
    private String autor;
    private Double avaliacao;
}
