package br.com.lumilivre.api.controller;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import br.com.lumilivre.api.config.SwaggerTags;
import br.com.lumilivre.api.dto.thesis.ThesisRequest;
import br.com.lumilivre.api.dto.thesis.ThesisResponse;
import br.com.lumilivre.api.mapper.ThesisMapper;
import br.com.lumilivre.api.service.ThesisService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/theses")
@RequiredArgsConstructor
@Tag(name = SwaggerTags.THESES)
public class ThesisController {

    private final ThesisService thesisService;
    private final ThesisMapper mapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @GetMapping
    @Operation(operationId = "theses.list")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN','READER')")
    public ResponseEntity<List<ThesisResponse>> list(
            @RequestParam(required = false) String q,
            Locale locale) {
        List<ThesisResponse> body = thesisService.listTheses(q).stream().map(mapper::toResponse).toList();
        return ResponseEntity.ok()
                .header("Content-Language", locale.toLanguageTag())
                .body(body);
    }

    @GetMapping("/{id}")
    @Operation(operationId = "theses.get")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN','READER')")
    public ResponseEntity<ThesisResponse> getOne(@PathVariable UUID id, Locale locale) {
        return ResponseEntity.ok()
                .header("Content-Language", locale.toLanguageTag())
                .body(mapper.toResponse(thesisService.getThesisById(id)));
    }

    @GetMapping("/search")
    @Operation(operationId = "theses.search")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN','READER')")
    public ResponseEntity<List<ThesisResponse>> search(
            @RequestParam(required = false) Integer courseId,
            @RequestParam(required = false) String semester,
            @RequestParam(required = false) String year,
            Locale locale) {
        List<ThesisResponse> body = thesisService.searchTheses(courseId, semester, year)
                .stream().map(mapper::toResponse).toList();
        return ResponseEntity.ok()
                .header("Content-Language", locale.toLanguageTag())
                .body(body);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(operationId = "theses.create")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public ResponseEntity<ThesisResponse> create(
            @RequestPart("data") String data,
            @RequestPart(value = "pdfFile", required = false) MultipartFile pdfFile,
            @RequestPart(value = "coverFile", required = false) MultipartFile coverFile,
            Locale locale) {
        ThesisRequest req = parseRequest(data);
        return ResponseEntity.status(201)
                .header("Content-Language", locale.toLanguageTag())
                .body(mapper.toResponse(thesisService.createThesis(req, pdfFile, coverFile)));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(operationId = "theses.update")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public ResponseEntity<ThesisResponse> update(
            @PathVariable UUID id,
            @RequestPart("data") String data,
            @RequestPart(value = "pdfFile", required = false) MultipartFile pdfFile,
            @RequestPart(value = "coverFile", required = false) MultipartFile coverFile,
            Locale locale) {
        ThesisRequest req = parseRequest(data);
        return ResponseEntity.ok()
                .header("Content-Language", locale.toLanguageTag())
                .body(mapper.toResponse(thesisService.updateThesis(id, req, pdfFile, coverFile)));
    }

    @DeleteMapping("/{id}")
    @Operation(operationId = "theses.delete")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        thesisService.deleteThesis(id);
        return ResponseEntity.noContent().build();
    }

    private ThesisRequest parseRequest(String data) {
        try {
            return objectMapper.readValue(data, ThesisRequest.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid thesis data: " + e.getMessage(), e);
        }
    }
}
