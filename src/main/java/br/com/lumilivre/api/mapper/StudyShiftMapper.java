package br.com.lumilivre.api.mapper;

import br.com.lumilivre.api.dto.studyshift.StudyShiftResponse;
import br.com.lumilivre.api.model.StudyShift;
import org.springframework.stereotype.Component;

@Component
public class StudyShiftMapper {

    public StudyShiftResponse toResponse(StudyShift entity) {
        return new StudyShiftResponse(entity.getId(), entity.getName());
    }
}
