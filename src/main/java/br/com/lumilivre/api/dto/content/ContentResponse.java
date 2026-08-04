package br.com.lumilivre.api.dto.content;

import java.time.OffsetDateTime;
import java.util.UUID;

import br.com.lumilivre.api.dto.common.LocalizedEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Representacao completa de um conteudo para o painel admin (web).
 * Enums vem localizados ({@link LocalizedEnum}: {@code code} + {@code label}).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContentResponse {

    private UUID id;
    private LocalizedEnum contentType;
    private String title;
    private String body;
    private String authors;
    private String advisors;
    private String completionYear;
    private String completionSemester;
    private String coverUrl;
    private String fileUrl;
    private String externalUrl;

    // ---- Visibilidade -------------------------------------------------------
    private Boolean published;
    private Boolean pinned;
    private Integer displayOrder;
    private LocalizedEnum audienceScope;
    private Integer courseId;
    private String courseName;
    private Integer academicModuleId;
    private String academicModuleName;
    private Integer studyShiftId;
    private String studyShiftName;
    private OffsetDateTime publishStartAt;
    private OffsetDateTime publishEndAt;

    /** Estado derivado (PUBLISHED/SCHEDULED/EXPIRED/HIDDEN) para o badge. */
    private LocalizedEnum status;

    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
