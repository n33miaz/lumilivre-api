package br.com.lumilivre.api.mapper;

import java.time.OffsetDateTime;
import java.util.Locale;

import org.springframework.stereotype.Component;

import br.com.lumilivre.api.dto.common.LocalizedEnum;
import br.com.lumilivre.api.dto.content.ContentFeedItemResponse;
import br.com.lumilivre.api.dto.content.ContentResponse;
import br.com.lumilivre.api.enums.ContentStatus;
import br.com.lumilivre.api.model.AppContent;
import br.com.lumilivre.api.service.EnumLabelResolver;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ContentMapper {

    private final EnumLabelResolver enumLabels;

    public ContentResponse toResponse(AppContent c, Locale locale) {
        ContentStatus status = ContentStatus.resolve(
                Boolean.TRUE.equals(c.getPublished()),
                c.getPublishStartAt(), c.getPublishEndAt(), OffsetDateTime.now());

        return ContentResponse.builder()
                .id(c.getId())
                .contentType(LocalizedEnum.of(c.getContentType(), enumLabels.resolve(c.getContentType(), locale)))
                .title(c.getTitle())
                .body(c.getBody())
                .authors(c.getAuthors())
                .advisors(c.getAdvisors())
                .completionYear(c.getCompletionYear() != null ? c.getCompletionYear().toString() : null)
                .completionSemester(c.getCompletionSemester())
                .coverUrl(c.getCoverUrl())
                .fileUrl(c.getFileUrl())
                .externalUrl(c.getExternalUrl())
                .published(c.getPublished())
                .pinned(c.getPinned())
                .displayOrder(c.getDisplayOrder())
                .audienceScope(LocalizedEnum.of(c.getAudienceScope(), enumLabels.resolve(c.getAudienceScope(), locale)))
                .courseId(c.getCourse() != null ? c.getCourse().getId() : null)
                .courseName(c.getCourse() != null ? c.getCourse().getName() : null)
                .academicModuleId(c.getAcademicModule() != null ? c.getAcademicModule().getId() : null)
                .academicModuleName(c.getAcademicModule() != null ? c.getAcademicModule().getName() : null)
                .studyShiftId(c.getStudyShift() != null ? c.getStudyShift().getId() : null)
                .studyShiftName(c.getStudyShift() != null ? c.getStudyShift().getName() : null)
                .publishStartAt(c.getPublishStartAt())
                .publishEndAt(c.getPublishEndAt())
                .status(LocalizedEnum.of(status, enumLabels.resolve(status, locale)))
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .build();
    }

    public ContentFeedItemResponse toFeedItem(AppContent c) {
        return ContentFeedItemResponse.builder()
                .id(c.getId())
                .contentType(c.getContentType() != null ? c.getContentType().name() : null)
                .title(c.getTitle())
                .body(c.getBody())
                .authors(c.getAuthors())
                .advisors(c.getAdvisors())
                .completionYear(c.getCompletionYear() != null ? c.getCompletionYear().toString() : null)
                .completionSemester(c.getCompletionSemester())
                .coverUrl(c.getCoverUrl())
                .fileUrl(c.getFileUrl())
                .externalUrl(c.getExternalUrl())
                .pinned(c.getPinned())
                .createdAt(c.getCreatedAt())
                .build();
    }
}
