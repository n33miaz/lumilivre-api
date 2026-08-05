package br.com.lumilivre.api.dto.user;

import java.util.UUID;

import br.com.lumilivre.api.dto.common.LocalizedEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSummaryResponse {

    private UUID id;
    private String email;
    private LocalizedEnum role;
    // A aba de Usuários precisa dos dois estados para desenhar o toggle sem
    // uma segunda chamada por linha.
    private Boolean active;
    private Boolean locked;
}
