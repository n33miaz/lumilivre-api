package br.com.lumilivre.api.mapper.v2;

import br.com.lumilivre.api.dto.academicmodule.AcademicModuleRequest;
import br.com.lumilivre.api.dto.academicmodule.AcademicModuleResponse;
import br.com.lumilivre.api.dto.academicmodule.AcademicModuleSummaryResponse;
import br.com.lumilivre.api.dto.v1.modulo.ModuloRequest;
import br.com.lumilivre.api.dto.v1.modulo.ModuloResponse;
import br.com.lumilivre.api.dto.v1.modulo.ModuloResumoResponse;
import org.springframework.stereotype.Component;

@Component
public class AcademicModuleMapper {

    public AcademicModuleSummaryResponse toSummary(ModuloResumoResponse v1) {
        return new AcademicModuleSummaryResponse(v1.getId(), v1.getNome(), v1.getQuantidadeAlunos());
    }

    public AcademicModuleResponse toResponse(ModuloResponse v1) {
        return new AcademicModuleResponse(v1.getId(), v1.getNome());
    }

    public ModuloRequest toV1Request(AcademicModuleRequest req) {
        return ModuloRequest.builder().nome(req.getName()).build();
    }
}
