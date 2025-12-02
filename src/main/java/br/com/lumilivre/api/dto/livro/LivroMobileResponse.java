package br.com.lumilivre.api.dto.livro;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LivroMobileResponse {

    private Long id;
    private String imagem;
    private String titulo;
    private String autor;
    private Double avaliacao; 
}