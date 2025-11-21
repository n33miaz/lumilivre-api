package br.com.lumilivre.api.dto.solicitacao;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SolicitacaoDashboardResponse {

    private String alunoNome;
    private String livroNome;
    private String tombo;
    private LocalDateTime dataSolicitacao;
}