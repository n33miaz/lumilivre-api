package br.com.lumilivre.api.mapper.v2;

import br.com.lumilivre.api.dto.thesis.ThesisResponse;
import br.com.lumilivre.api.model.Thesis;
import org.springframework.stereotype.Component;

@Component
public class ThesisMapper {

    public ThesisResponse toResponse(Thesis entity) {
        return ThesisResponse.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .authors(entity.getAuthors())
                .advisors(entity.getAdvisors())
                .courseName(entity.getCourse() != null ? entity.getCourse().getName() : null)
                .completionYear(entity.getCompletionYear() != null ? entity.getCompletionYear().toString() : null)
                .completionSemester(entity.getCompletionSemester())
                .pdfUrl(entity.getPdfUrl())
                .coverUrl(entity.getCoverUrl())
                .externalUrl(entity.getExternalUrl())
                .active(entity.getActive())
                .build();
    }
}
