package br.com.lumilivre.api.dto.modulo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ModuloResumoResponse {
    private Integer id;
    private String nome;
    private Long quantidadeAlunos;
}