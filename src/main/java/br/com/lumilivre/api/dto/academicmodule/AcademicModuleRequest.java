package br.com.lumilivre.api.dto.academicmodule;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AcademicModuleRequest {

    @NotBlank
    private String name;
}
