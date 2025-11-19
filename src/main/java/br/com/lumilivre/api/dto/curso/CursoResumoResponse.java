package br.com.lumilivre.api.dto.curso;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CursoResumoResponse {

    private Integer id;
    private String nome;
    private Long quantidadeAlunos;

}