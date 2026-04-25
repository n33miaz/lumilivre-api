package br.com.lumilivre.api.dto.livro;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LivroAgrupadoResponse {

    private UUID id;
    private String isbn;
    private String nome;
    private String autor;
    private String editora;
    private Long quantidade;
}
