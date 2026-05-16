package br.com.lumilivre.api.controller.v2;

import java.util.Locale;
import java.util.List;

import br.com.lumilivre.api.dto.academicmodule.AcademicModuleRequest;
import br.com.lumilivre.api.dto.academicmodule.AcademicModuleResponse;
import br.com.lumilivre.api.dto.academicmodule.AcademicModuleSummaryResponse;
import br.com.lumilivre.api.dto.common.ChartItemResponse;
import br.com.lumilivre.api.mapper.v2.AcademicModuleMapper;
import br.com.lumilivre.api.service.AcademicModuleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v2/academic-modules")
@RequiredArgsConstructor
public class AcademicModuleController {

    private final AcademicModuleService academicModuleService;
    private final AcademicModuleMapper mapper;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN','STUDENT')")
    public ResponseEntity<Page<AcademicModuleSummaryResponse>> list(
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20) Pageable pageable,
            Locale locale) {
        Page<AcademicModuleSummaryResponse> page = academicModuleService.buscarPorTexto(q, pageable);
        return ResponseEntity.ok()
                .header("Content-Language", locale.toLanguageTag())
                .body(page);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public ResponseEntity<AcademicModuleResponse> create(
            @Valid @RequestBody AcademicModuleRequest request,
            Locale locale) {
        AcademicModuleResponse body = mapper.toResponse(academicModuleService.cadastrar(request));
        return ResponseEntity.status(201)
                .header("Content-Language", locale.toLanguageTag())
                .body(body);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public ResponseEntity<AcademicModuleResponse> update(
            @PathVariable Integer id,
            @Valid @RequestBody AcademicModuleRequest request,
            Locale locale) {
        AcademicModuleResponse body = mapper.toResponse(academicModuleService.atualizar(id, request));
        return ResponseEntity.ok()
                .header("Content-Language", locale.toLanguageTag())
                .body(body);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        academicModuleService.excluir(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/loan-statistics")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public ResponseEntity<List<ChartItemResponse>> loanStatistics(Locale locale) {
        return ResponseEntity.ok()
                .header("Content-Language", locale.toLanguageTag())
                .body(academicModuleService.buscarTotalEmprestimosPorModulo());
    }
}
