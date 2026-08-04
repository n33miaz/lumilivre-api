package br.com.lumilivre.api.controller;

import java.time.OffsetDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.lumilivre.api.config.SwaggerTags;
import br.com.lumilivre.api.dto.audit.AccessLogResponse;
import br.com.lumilivre.api.model.AccessLog;
import br.com.lumilivre.api.service.AccessLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/access-logs")
@RequiredArgsConstructor
@Tag(name = SwaggerTags.AUDIT)
public class AccessLogController {

    private final AccessLogService accessLogService;

    @GetMapping
    @Operation(operationId = "access.list")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<AccessLogResponse>> list(
            @RequestParam(required = false) String event,
            @RequestParam(required = false) String channel,
            @RequestParam(required = false) String result,
            @RequestParam(required = false) String actor,
            @RequestParam(required = false) String ip,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<AccessLogResponse> body = accessLogService
                .search(event, channel, result, actor, ip, from, to, pageable)
                .map(this::toResponse);
        return ResponseEntity.ok(body);
    }

    private AccessLogResponse toResponse(AccessLog a) {
        return new AccessLogResponse(
                a.getId(), a.getActor(), a.getActorRole(), a.getEvent(), a.getChannel(),
                a.getResult(), a.getIpAddress(), a.getUserAgent(), a.getCorrelationId(),
                a.getErrorMessage(), a.getOccurredAt());
    }
}
