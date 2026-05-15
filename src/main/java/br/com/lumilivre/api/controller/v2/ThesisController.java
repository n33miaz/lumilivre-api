package br.com.lumilivre.api.controller.v2;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import br.com.lumilivre.api.dto.thesis.ThesisRequest;
import br.com.lumilivre.api.dto.thesis.ThesisResponse;
import br.com.lumilivre.api.mapper.v2.ThesisMapper;
import br.com.lumilivre.api.service.ThesisService;
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
@RequestMapping("/api/v2/theses")
@RequiredArgsConstructor
public class ThesisController {

    private final ThesisService thesisService;
    private final ThesisMapper mapper;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN','STUDENT')")
    public ResponseEntity<List<ThesisResponse>> list(
            @RequestParam(required = false) String q,
            Locale locale) {
        var v1 = thesisService.listTheses(q);
        List<ThesisResponse> body = v1.getBody() != null && v1.getBody().getData() != null
                ? v1.getBody().getData().stream().map(mapper::fromV1).toList()
                : List.of();
        return ResponseEntity.ok()
                .header("Content-Language", locale.toLanguageTag())
                .body(body);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN','STUDENT')")
    public ResponseEntity<ThesisResponse> getOne(@PathVariable UUID id, Locale locale) {
        var v1 = thesisService.getThesisById(id);
        if (!v1.getStatusCode().is2xxSuccessful() || v1.getBody() == null || v1.getBody().getData() == null) {
            return ResponseEntity.status(v1.getStatusCode())
                    .header("Content-Language", locale.toLanguageTag())
                    .build();
        }
        return ResponseEntity.ok()
                .header("Content-Language", locale.toLanguageTag())
                .body(mapper.fromV1(v1.getBody().getData()));
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN','STUDENT')")
    public ResponseEntity<List<ThesisResponse>> search(
            @RequestParam(required = false) Integer courseId,
            @RequestParam(required = false) String semester,
            @RequestParam(required = false) String year,
            Locale locale) {
        var v1 = thesisService.searchTheses(courseId, semester, year);
        List<ThesisResponse> body = v1.getBody() != null && v1.getBody().getData() != null
                ? v1.getBody().getData().stream().map(mapper::fromV1).toList()
                : List.of();
        return ResponseEntity.ok()
                .header("Content-Language", locale.toLanguageTag())
                .body(body);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public ResponseEntity<ThesisResponse> create(
            @RequestPart("data") String data,
            @RequestPart(value = "pdfFile", required = false) MultipartFile pdfFile,
            @RequestPart(value = "coverFile", required = false) MultipartFile coverFile,
            Locale locale) {
        ThesisRequest req = parseRequest(data);
        var v1 = thesisService.createThesis(mapper.toV1Json(req), pdfFile, coverFile);
        if (!v1.getStatusCode().is2xxSuccessful() || v1.getBody() == null || v1.getBody().getData() == null) {
            return ResponseEntity.status(v1.getStatusCode())
                    .header("Content-Language", locale.toLanguageTag())
                    .build();
        }
        return ResponseEntity.status(201)
                .header("Content-Language", locale.toLanguageTag())
                .body(mapper.fromV1(v1.getBody().getData()));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public ResponseEntity<ThesisResponse> update(
            @PathVariable UUID id,
            @RequestPart("data") String data,
            @RequestPart(value = "pdfFile", required = false) MultipartFile pdfFile,
            @RequestPart(value = "coverFile", required = false) MultipartFile coverFile,
            Locale locale) {
        ThesisRequest req = parseRequest(data);
        var v1 = thesisService.updateThesis(id, mapper.toV1Json(req), pdfFile, coverFile);
        if (!v1.getStatusCode().is2xxSuccessful() || v1.getBody() == null || v1.getBody().getData() == null) {
            return ResponseEntity.status(v1.getStatusCode())
                    .header("Content-Language", locale.toLanguageTag())
                    .build();
        }
        return ResponseEntity.ok()
                .header("Content-Language", locale.toLanguageTag())
                .body(mapper.fromV1(v1.getBody().getData()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        thesisService.deleteThesis(id);
        return ResponseEntity.noContent().build();
    }

    private ThesisRequest parseRequest(String data) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
            return om.readValue(data, ThesisRequest.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid thesis data: " + e.getMessage(), e);
        }
    }
}
