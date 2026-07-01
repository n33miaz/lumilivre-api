package br.com.lumilivre.api.controller;

import java.util.List;
import java.util.Locale;

import br.com.lumilivre.api.config.SwaggerTags;
import br.com.lumilivre.api.dto.reader.ReaderRankingResponse;
import br.com.lumilivre.api.dto.reader.ReaderRequest;
import br.com.lumilivre.api.dto.reader.ReaderResponse;
import br.com.lumilivre.api.dto.reader.ReaderSummaryResponse;
import br.com.lumilivre.api.mapper.ReaderMapper;
import br.com.lumilivre.api.security.CanAccessReader;
import br.com.lumilivre.api.service.ReaderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@RequestMapping("/api/readers")
@RequiredArgsConstructor
@Tag(name = SwaggerTags.READERS)
public class ReaderController {

    private final ReaderService readerService;
    private final ReaderMapper mapper;

    @GetMapping
    @Operation(operationId = "readers.list")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public ResponseEntity<Page<ReaderSummaryResponse>> list(
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20) Pageable pageable,
            Locale locale) {
        Page<ReaderSummaryResponse> page = readerService
                .listarParaAdminV2(q, pageable)
                .map(item -> mapper.toSummary(item, locale));
        return ResponseEntity.ok()
                .header("Content-Language", locale.toLanguageTag())
                .body(page);
    }

    @GetMapping("/search")
    @Operation(operationId = "readers.search")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public ResponseEntity<Page<ReaderSummaryResponse>> search(
            @RequestParam(required = false) String penalty,
            @RequestParam(required = false) String registrationNumber,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String courseName,
            @RequestParam(required = false) Integer studyShiftId,
            @RequestParam(required = false) Integer academicModuleId,
            @PageableDefault(size = 20) Pageable pageable,
            Locale locale) {
        Page<ReaderSummaryResponse> page = readerService
                .buscarAvancadoV2(penalty, registrationNumber, name, courseName, studyShiftId, academicModuleId,
                        null, null, null, pageable)
                .map(item -> mapper.toSummary(item, locale));
        return ResponseEntity.ok()
                .header("Content-Language", locale.toLanguageTag())
                .body(page);
    }

    @GetMapping("/{registrationNumber}")
    @Operation(operationId = "readers.get")
    @CanAccessReader
    public ResponseEntity<ReaderResponse> getOne(
            @PathVariable String registrationNumber,
            Locale locale) {
        ReaderResponse body = mapper.toResponse(
                readerService.buscarPorMatricula(registrationNumber), locale);
        return ResponseEntity.ok()
                .header("Content-Language", locale.toLanguageTag())
                .body(body);
    }

    @PostMapping
    @Operation(operationId = "readers.create")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public ResponseEntity<ReaderResponse> create(
            @Valid @RequestBody ReaderRequest request,
            Locale locale) {
        ReaderResponse body = mapper.toResponse(
                readerService.cadastrar(request), locale);
        return ResponseEntity.status(201)
                .header("Content-Language", locale.toLanguageTag())
                .body(body);
    }

    @PutMapping("/{registrationNumber}")
    @Operation(operationId = "readers.update")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public ResponseEntity<ReaderResponse> update(
            @PathVariable String registrationNumber,
            @Valid @RequestBody ReaderRequest request,
            Locale locale) {
        ReaderResponse body = mapper.toResponse(
                readerService.atualizar(registrationNumber, request), locale);
        return ResponseEntity.ok()
                .header("Content-Language", locale.toLanguageTag())
                .body(body);
    }

    @DeleteMapping("/{registrationNumber}")
    @Operation(operationId = "readers.delete")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public ResponseEntity<Void> delete(@PathVariable String registrationNumber) {
        readerService.excluir(registrationNumber);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{registrationNumber}/reset-password")
    @Operation(operationId = "readers.resetPassword")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public ResponseEntity<Void> resetPassword(@PathVariable String registrationNumber) {
        readerService.resetarSenha(registrationNumber);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/{registrationNumber}/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(operationId = "readers.uploadAvatar")
    @CanAccessReader
    public ResponseEntity<Void> uploadAvatar(
            @PathVariable String registrationNumber,
            @RequestPart("file") MultipartFile file,
            Locale locale) {
        readerService.uploadFoto(registrationNumber, file);
        return ResponseEntity.noContent()
                .header("Content-Language", locale.toLanguageTag())
                .build();
    }

    @GetMapping("/ranking")
    @Operation(operationId = "readers.ranking")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN','READER')")
    public ResponseEntity<List<ReaderRankingResponse>> ranking(
            @RequestParam(defaultValue = "10") int top,
            @RequestParam(required = false) Integer courseId,
            @RequestParam(required = false) Integer academicModuleId,
            @RequestParam(required = false) Integer studyShiftId,
            Locale locale) {
        List<ReaderRankingResponse> body = readerService
                .gerarRankingLeitoresV2(top, courseId, academicModuleId, studyShiftId)
                .stream()
                .map(mapper::toRanking)
                .toList();
        return ResponseEntity.ok()
                .header("Content-Language", locale.toLanguageTag())
                .body(body);
    }
}
