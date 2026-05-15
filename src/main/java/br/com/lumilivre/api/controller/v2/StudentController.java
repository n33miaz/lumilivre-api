package br.com.lumilivre.api.controller.v2;

import java.util.List;
import java.util.Locale;

import br.com.lumilivre.api.dto.student.StudentRankingResponse;
import br.com.lumilivre.api.dto.student.StudentRequest;
import br.com.lumilivre.api.dto.student.StudentResponse;
import br.com.lumilivre.api.dto.student.StudentSummaryResponse;
import br.com.lumilivre.api.mapper.v2.StudentMapper;
import br.com.lumilivre.api.security.CanAccessStudent;
import br.com.lumilivre.api.service.StudentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.MediaType;

@RestController
@RequestMapping("/api/v2/students")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;
    private final StudentMapper mapper;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public ResponseEntity<Page<StudentSummaryResponse>> list(
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20) Pageable pageable,
            Locale locale) {
        Page<StudentSummaryResponse> page = studentService
                .listarParaAdminV2(q, pageable)
                .map(item -> mapper.toSummary(item, locale));
        return ResponseEntity.ok()
                .header("Content-Language", locale.toLanguageTag())
                .body(page);
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public ResponseEntity<Page<StudentSummaryResponse>> search(
            @RequestParam(required = false) String penalty,
            @RequestParam(required = false) String registrationNumber,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String courseName,
            @RequestParam(required = false) Integer studyShiftId,
            @RequestParam(required = false) Integer academicModuleId,
            @PageableDefault(size = 20) Pageable pageable,
            Locale locale) {
        Page<StudentSummaryResponse> page = studentService
                .buscarAvancadoV2(penalty, registrationNumber, name, courseName, studyShiftId, academicModuleId,
                        null, null, null, pageable)
                .map(item -> mapper.toSummary(item, locale));
        return ResponseEntity.ok()
                .header("Content-Language", locale.toLanguageTag())
                .body(page);
    }

    @GetMapping("/{registrationNumber}")
    @CanAccessStudent
    public ResponseEntity<StudentResponse> getOne(
            @PathVariable String registrationNumber,
            Locale locale) {
        StudentResponse body = mapper.toResponse(
                studentService.buscarPorMatricula(registrationNumber), locale);
        return ResponseEntity.ok()
                .header("Content-Language", locale.toLanguageTag())
                .body(body);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public ResponseEntity<StudentResponse> create(
            @Valid @RequestBody StudentRequest request,
            Locale locale) {
        StudentResponse body = mapper.toResponse(
                studentService.cadastrar(request), locale);
        return ResponseEntity.status(201)
                .header("Content-Language", locale.toLanguageTag())
                .body(body);
    }

    @PutMapping("/{registrationNumber}")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public ResponseEntity<StudentResponse> update(
            @PathVariable String registrationNumber,
            @Valid @RequestBody StudentRequest request,
            Locale locale) {
        StudentResponse body = mapper.toResponse(
                studentService.atualizar(registrationNumber, request), locale);
        return ResponseEntity.ok()
                .header("Content-Language", locale.toLanguageTag())
                .body(body);
    }

    @DeleteMapping("/{registrationNumber}")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public ResponseEntity<Void> delete(@PathVariable String registrationNumber) {
        studentService.excluir(registrationNumber);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{registrationNumber}/reset-password")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public ResponseEntity<Void> resetPassword(@PathVariable String registrationNumber) {
        studentService.resetarSenha(registrationNumber);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/{registrationNumber}/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @CanAccessStudent
    public ResponseEntity<Void> uploadAvatar(
            @PathVariable String registrationNumber,
            @RequestPart("file") MultipartFile file,
            Locale locale) {
        studentService.uploadFoto(registrationNumber, file);
        return ResponseEntity.noContent()
                .header("Content-Language", locale.toLanguageTag())
                .build();
    }

    @GetMapping("/ranking")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN','STUDENT')")
    public ResponseEntity<List<StudentRankingResponse>> ranking(
            @RequestParam(defaultValue = "10") int top,
            @RequestParam(required = false) Integer courseId,
            @RequestParam(required = false) Integer academicModuleId,
            @RequestParam(required = false) Integer studyShiftId,
            Locale locale) {
        List<StudentRankingResponse> body = studentService
                .gerarRankingAlunosV2(top, courseId, academicModuleId, studyShiftId)
                .stream()
                .map(mapper::toRanking)
                .toList();
        return ResponseEntity.ok()
                .header("Content-Language", locale.toLanguageTag())
                .body(body);
    }
}
