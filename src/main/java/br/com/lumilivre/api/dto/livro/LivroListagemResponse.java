package br.com.lumilivre.api.dto.livro;

import br.com.lumilivre.api.enums.StatusLivro;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LivroListagemResponse {

    private StatusLivro status;
    private String tomboExemplar;
    private String isbn;
    private String cdd;
    private String nome;
    private String genero;
    private String autor;
    private String editora;
    private String localizacao_fisica;
}