package br.com.lumilivre.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ListaTurnoDTO {
    private Integer id;
    private String nome;
    private Long quantidadeAlunos;
}