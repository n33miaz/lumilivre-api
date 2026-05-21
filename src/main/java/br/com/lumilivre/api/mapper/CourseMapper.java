package br.com.lumilivre.api.mapper;

import br.com.lumilivre.api.dto.course.CourseResponse;
import br.com.lumilivre.api.model.Course;
import org.springframework.stereotype.Component;

@Component
public class CourseMapper {

    public CourseResponse toResponse(Course entity) {
        return new CourseResponse(entity.getId(), entity.getName());
    }
}
