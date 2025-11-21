package br.com.lumilivre.api.dto.solicitacao;

import java.time.LocalDateTime;

import br.com.lumilivre.api.enums.StatusSolicitacao;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SolicitacaoResponse {

    private Integer id;
    private String alunoNome;
    private String alunoMatricula;
    private String exemplarTombo;
    private String livroNome;
    private LocalDateTime dataSolicitacao;
    private StatusSolicitacao status;
    private String observacao;
}