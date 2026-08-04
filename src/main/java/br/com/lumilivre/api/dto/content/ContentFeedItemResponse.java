package br.com.lumilivre.api.dto.content;

import java.time.OffsetDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Item do mural do app (leitor). Enxuto e com codigos crus de enum — o app
 * localiza os rotulos. Nao expoe segmentacao nem janela (o feed ja veio filtrado).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContentFeedItemResponse {

    private UUID id;
    /** Codigo do tipo: ANNOUNCEMENT / ATTACHMENT / WORK. */
    private String contentType;
    private String title;
    private String body;
    private String authors;
    private String advisors;
    private String completionYear;
    private String completionSemester;
    private String coverUrl;
    private String fileUrl;
    private String externalUrl;
    private Boolean pinned;
    private OffsetDateTime createdAt;
}
