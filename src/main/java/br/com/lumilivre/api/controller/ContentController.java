package br.com.lumilivre.api.controller;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

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

import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.lumilivre.api.config.SwaggerTags;
import br.com.lumilivre.api.dto.content.ContentFeedItemResponse;
import br.com.lumilivre.api.dto.content.ContentRequest;
import br.com.lumilivre.api.dto.content.ContentResponse;
import br.com.lumilivre.api.enums.AudienceScope;
import br.com.lumilivre.api.enums.ContentType;
import br.com.lumilivre.api.mapper.ContentMapper;
import br.com.lumilivre.api.service.AppContentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/contents")
@RequiredArgsConstructor
@Tag(name = SwaggerTags.CONTENTS)
public class ContentController {

    private final AppContentService contentService;
    private final ContentMapper mapper;
    private final ObjectMapper objectMapper;

    @GetMapping
    @Operation(operationId = "contents.list")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public ResponseEntity<List<ContentResponse>> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) ContentType type,
            Locale locale) {
        List<ContentResponse> body = contentService.listForAdmin(q, type)
                .stream().map(c -> mapper.toResponse(c, locale)).toList();
        return ResponseEntity.ok()
                .header("Content-Language", locale.toLanguageTag())
                .body(body);
    }

    @GetMapping("/search")
    @Operation(operationId = "contents.search")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public ResponseEntity<List<ContentResponse>> search(
            @RequestParam(required = false) ContentType type,
            @RequestParam(required = false) AudienceScope scope,
            @RequestParam(required = false) Integer courseId,
            @RequestParam(required = false) String year,
            Locale locale) {
        List<ContentResponse> body = contentService.searchAdvanced(type, scope, courseId, year)
                .stream().map(c -> mapper.toResponse(c, locale)).toList();
        return ResponseEntity.ok()
                .header("Content-Language", locale.toLanguageTag())
                .body(body);
    }

    @GetMapping("/feed")
    @Operation(operationId = "contents.feed")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN','READER')")
    public ResponseEntity<List<ContentFeedItemResponse>> feed(Locale locale) {
        List<ContentFeedItemResponse> body = contentService.feedForCurrentReader()
                .stream().map(mapper::toFeedItem).toList();
        return ResponseEntity.ok()
                .header("Content-Language", locale.toLanguageTag())
                .body(body);
    }

    @GetMapping("/{id}")
    @Operation(operationId = "contents.get")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN','READER')")
    public ResponseEntity<ContentResponse> getOne(@PathVariable UUID id, Locale locale) {
        return ResponseEntity.ok()
                .header("Content-Language", locale.toLanguageTag())
                .body(mapper.toResponse(contentService.getById(id), locale));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(operationId = "contents.create")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public ResponseEntity<ContentResponse> create(
            @RequestPart("data") String data,
            @RequestPart(value = "coverFile", required = false) MultipartFile coverFile,
            @RequestPart(value = "docFile", required = false) MultipartFile docFile,
            Locale locale) {
        ContentRequest req = parseRequest(data);
        return ResponseEntity.status(201)
                .header("Content-Language", locale.toLanguageTag())
                .body(mapper.toResponse(contentService.create(req, coverFile, docFile), locale));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(operationId = "contents.update")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public ResponseEntity<ContentResponse> update(
            @PathVariable UUID id,
            @RequestPart("data") String data,
            @RequestPart(value = "coverFile", required = false) MultipartFile coverFile,
            @RequestPart(value = "docFile", required = false) MultipartFile docFile,
            Locale locale) {
        ContentRequest req = parseRequest(data);
        return ResponseEntity.ok()
                .header("Content-Language", locale.toLanguageTag())
                .body(mapper.toResponse(contentService.update(id, req, coverFile, docFile), locale));
    }

    @DeleteMapping("/{id}")
    @Operation(operationId = "contents.delete")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        contentService.delete(id);
        return ResponseEntity.noContent().build();
    }

    private ContentRequest parseRequest(String data) {
        try {
            return objectMapper.readValue(data, ContentRequest.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid content data: " + e.getMessage(), e);
        }
    }
}
