package br.com.lumilivre.api.dto.solicitacao;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SolicitacaoResumoResponse {

    private String alunoNome;
    private String livroNome;
    private LocalDateTime dataSolicitacao;
}