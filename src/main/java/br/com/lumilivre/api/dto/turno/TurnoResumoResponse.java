package br.com.lumilivre.api.dto.turno;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TurnoResumoResponse {
    
    private Integer id;
    private String nome;
    private Long quantidadeAlunos;
}