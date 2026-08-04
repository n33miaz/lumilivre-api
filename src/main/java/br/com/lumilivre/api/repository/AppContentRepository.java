package br.com.lumilivre.api.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.lumilivre.api.enums.AudienceScope;
import br.com.lumilivre.api.enums.ContentType;
import br.com.lumilivre.api.model.AppContent;

public interface AppContentRepository extends JpaRepository<AppContent, UUID> {

    /**
     * Lista para o painel admin: todos os conteudos nao removidos (inclusive
     * despublicados/agendados), com filtro textual e por tipo opcionais.
     */
    @Query("""
            SELECT c FROM AppContent c
            LEFT JOIN FETCH c.course
            LEFT JOIN FETCH c.academicModule
            LEFT JOIN FETCH c.studyShift
            WHERE c.deletedAt IS NULL
              AND (:q IS NULL OR LOWER(c.title) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(COALESCE(c.authors, '')) LIKE LOWER(CONCAT('%', :q, '%')))
              AND (:type IS NULL OR c.contentType = :type)
            ORDER BY c.pinned DESC, c.displayOrder ASC, c.createdAt DESC
            """)
    List<AppContent> findForAdmin(@Param("q") String q, @Param("type") ContentType type);

    /**
     * Filtro avancado do painel admin.
     */
    @Query("""
            SELECT c FROM AppContent c
            LEFT JOIN FETCH c.course
            LEFT JOIN FETCH c.academicModule
            LEFT JOIN FETCH c.studyShift
            WHERE c.deletedAt IS NULL
              AND (:type IS NULL OR c.contentType = :type)
              AND (:scope IS NULL OR c.audienceScope = :scope)
              AND (:courseId IS NULL OR c.course.id = :courseId)
              AND (:year IS NULL OR c.completionYear = :year)
            ORDER BY c.pinned DESC, c.displayOrder ASC, c.createdAt DESC
            """)
    List<AppContent> searchAdvanced(@Param("type") ContentType type,
                                    @Param("scope") AudienceScope scope,
                                    @Param("courseId") Integer courseId,
                                    @Param("year") Integer year);

    /**
     * Feed do leitor (mural do app): apenas publicados, dentro da janela e
     * direcionados ao publico do leitor. Ver WS-01 / regra do feed.
     */
    @Query("""
            SELECT c FROM AppContent c
            WHERE c.deletedAt IS NULL
              AND c.published = TRUE
              AND (c.publishStartAt IS NULL OR c.publishStartAt <= :now)
              AND (c.publishEndAt   IS NULL OR c.publishEndAt   >= :now)
              AND (c.audienceScope = br.com.lumilivre.api.enums.AudienceScope.ALL
                   OR (c.audienceScope = br.com.lumilivre.api.enums.AudienceScope.COURSE AND c.course.id = :courseId)
                   OR (c.audienceScope = br.com.lumilivre.api.enums.AudienceScope.MODULE AND c.academicModule.id = :moduleId)
                   OR (c.audienceScope = br.com.lumilivre.api.enums.AudienceScope.SHIFT  AND c.studyShift.id = :shiftId))
            ORDER BY c.pinned DESC, c.displayOrder ASC, c.createdAt DESC
            """)
    List<AppContent> findFeed(@Param("courseId") Integer courseId,
                              @Param("moduleId") Integer moduleId,
                              @Param("shiftId") Integer shiftId,
                              @Param("now") OffsetDateTime now);
}
