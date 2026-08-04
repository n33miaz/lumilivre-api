package br.com.lumilivre.api.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.lumilivre.api.model.AppContent;

// As listagens do painel (busca textual + filtro avançado) usam Specification
// dinâmica montada no AppContentService: o padrão JPQL "(:p IS NULL OR ...)"
// quebra no PostgreSQL para parâmetros nulos não-string ("could not determine
// data type of parameter").
public interface AppContentRepository
        extends JpaRepository<AppContent, UUID>, JpaSpecificationExecutor<AppContent> {

    /**
     * Feed do leitor (mural do app): apenas publicados, dentro da janela e
     * direcionados ao publico do leitor. Ver a regra do feed.
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
