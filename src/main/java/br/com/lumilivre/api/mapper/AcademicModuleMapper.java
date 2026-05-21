package br.com.lumilivre.api.mapper;

import br.com.lumilivre.api.dto.academicmodule.AcademicModuleResponse;
import br.com.lumilivre.api.model.AcademicModule;
import org.springframework.stereotype.Component;

@Component
public class AcademicModuleMapper {

    public AcademicModuleResponse toResponse(AcademicModule entity) {
        return new AcademicModuleResponse(entity.getId(), entity.getName());
    }
}
