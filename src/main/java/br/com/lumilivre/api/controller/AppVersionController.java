package br.com.lumilivre.api.controller;

import java.security.Principal;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.lumilivre.api.config.SwaggerTags;
import br.com.lumilivre.api.dto.appversion.AppVersionRequest;
import br.com.lumilivre.api.dto.appversion.AppVersionResponse;
import br.com.lumilivre.api.service.AppVersionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/app-version")
@RequiredArgsConstructor
@Tag(name = SwaggerTags.APP_VERSION)
public class AppVersionController {

    private final AppVersionService appVersionService;

    @GetMapping
    @Operation(operationId = "appVersion.get")
    public ResponseEntity<AppVersionResponse> get(
            @RequestParam(defaultValue = "ANDROID") String platform) {
        return ResponseEntity.ok(appVersionService.get(platform));
    }

    @PutMapping
    @Operation(operationId = "appVersion.update")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AppVersionResponse> update(
            @Valid @RequestBody AppVersionRequest request,
            Principal principal) {
        String updatedBy = principal != null ? principal.getName() : "unknown";
        return ResponseEntity.ok(appVersionService.update(request, updatedBy));
    }
}
