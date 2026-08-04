package br.com.lumilivre.api.model;

import java.time.OffsetDateTime;
import java.util.UUID;

import br.com.lumilivre.api.enums.AudienceScope;
import br.com.lumilivre.api.enums.ContentType;
import jakarta.persistence.*;
import lombok.*;

/**
 * Conteudo publicavel do ecossistema (comunicado, anexo ou trabalho/TCC).
 * Generaliza a antiga entidade {@code Thesis} — ver migration V8. Carrega os
 * metadados academicos (autores/orientadores/ano) usados por {@link ContentType#WORK}
 * e os quatro controles de visibilidade respeitados pelo mural do app.
 */
@Entity
@Table(name = "app_content")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppContent {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "uuid")
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "content_type", nullable = false, length = 20)
    private ContentType contentType;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "body", columnDefinition = "text")
    private String body;

    @Column(name = "authors", length = 500)
    private String authors;

    @Column(name = "advisors", length = 500)
    private String advisors;

    @Column(name = "completion_year")
    private Integer completionYear;

    @Column(name = "completion_semester", length = 10)
    private String completionSemester;

    @Column(name = "cover_url", length = 1024)
    private String coverUrl;

    @Column(name = "file_url", length = 1024)
    private String fileUrl;

    @Column(name = "external_url", length = 1024)
    private String externalUrl;

    // ---- Visibilidade -------------------------------------------------------

    @Builder.Default
    @Column(name = "is_published", nullable = false)
    private Boolean published = true;

    @Builder.Default
    @Column(name = "is_pinned", nullable = false)
    private Boolean pinned = false;

    @Builder.Default
    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "audience_scope", nullable = false, length = 20)
    private AudienceScope audienceScope = AudienceScope.ALL;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    @ToString.Exclude
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "academic_module_id")
    @ToString.Exclude
    private AcademicModule academicModule;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "study_shift_id")
    @ToString.Exclude
    private StudyShift studyShift;

    @Column(name = "publish_start_at")
    private OffsetDateTime publishStartAt;

    @Column(name = "publish_end_at")
    private OffsetDateTime publishEndAt;

    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Builder.Default
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;
}
