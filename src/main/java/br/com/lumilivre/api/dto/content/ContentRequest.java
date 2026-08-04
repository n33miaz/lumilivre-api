package br.com.lumilivre.api.dto.content;

import java.time.OffsetDateTime;

import br.com.lumilivre.api.enums.AudienceScope;
import br.com.lumilivre.api.enums.ContentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payload JSON (campo multipart {@code data}) de criacao/edicao de conteudo.
 * Espelha {@link br.com.lumilivre.api.model.AppContent}; arquivos (capa/documento)
 * vao em partes multipart separadas.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContentRequest {

    private ContentType contentType;
    private String title;
    private String body;
    private String authors;
    private String advisors;
    /** Recebido como texto para tolerar campo vazio no form (como o TCC legado). */
    private String completionYear;
    private String completionSemester;
    private String externalUrl;

    // ---- Visibilidade -------------------------------------------------------

    @Builder.Default
    private Boolean published = Boolean.TRUE;

    @Builder.Default
    private Boolean pinned = Boolean.FALSE;

    @Builder.Default
    private Integer displayOrder = 0;

    @Builder.Default
    private AudienceScope audienceScope = AudienceScope.ALL;

    private Integer courseId;
    private Integer academicModuleId;
    private Integer studyShiftId;

    private OffsetDateTime publishStartAt;
    private OffsetDateTime publishEndAt;
}
