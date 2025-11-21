package br.com.lumilivre.api.dto.livro;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LivroAgrupadoResponse {

    private Long id;
    private String isbn;
    private String nome;
    private String autor;
    private String editora;
    private Long quantidade;
}