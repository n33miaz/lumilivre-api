package br.com.lumilivre.api.dto.v1.solicitacao;

import java.time.OffsetDateTime;

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
    private OffsetDateTime dataSolicitacao;
}
