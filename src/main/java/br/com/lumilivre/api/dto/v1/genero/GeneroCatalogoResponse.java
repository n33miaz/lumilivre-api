package br.com.lumilivre.api.dto.v1.genero;

import java.util.List;

import br.com.lumilivre.api.dto.v1.livro.LivroMobileResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GeneroCatalogoResponse {

    private String nome;
    private List<LivroMobileResponse> livros;
}