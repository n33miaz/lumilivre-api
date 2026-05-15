package br.com.lumilivre.api.dto.v1.solicitacao;

import java.time.OffsetDateTime;
import java.util.UUID;

import br.com.lumilivre.api.enums.LoanRequestStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SolicitacaoResponse {

    private UUID id;
    private String alunoNome;
    private String alunoMatricula;
    private String exemplarTombo;

    private UUID livroId;
    private String livroNome;

    private OffsetDateTime dataSolicitacao;
    private LoanRequestStatus status;
    private String observacao;
}
