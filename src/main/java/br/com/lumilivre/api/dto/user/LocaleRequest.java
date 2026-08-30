package br.com.lumilivre.api.dto.user;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Corpo do {@code PATCH /api/users/me/locale}.
 *
 * <p>O idioma preferido decide em que lingua saem os e-mails transacionais
 * (recuperacao de senha, aviso de atraso), que rodam fora de uma requisicao e
 * portanto nao tem o cabecalho {@code Accept-Language} para se guiar. Sem este
 * endpoint a coluna {@code preferred_locale} nascia em pt-BR e nunca mudava, e
 * todo e-mail saia em portugues mesmo para quem usa o sistema em outro idioma.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocaleRequest {

    @NotBlank
    private String locale;
}
