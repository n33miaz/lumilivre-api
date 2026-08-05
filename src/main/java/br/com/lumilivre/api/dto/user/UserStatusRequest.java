package br.com.lumilivre.api.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Corpo do {@code PATCH /api/users/{id}/status}.
 *
 * <p>Os dois campos são opcionais e {@code null} significa "não mexer": a aba de
 * Usuários tem um toggle por coluna e não deve reenviar o estado do outro.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserStatusRequest {

    private Boolean active;
    private Boolean locked;
}
