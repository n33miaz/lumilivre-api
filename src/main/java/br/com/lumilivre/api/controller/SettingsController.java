package br.com.lumilivre.api.controller;

import br.com.lumilivre.api.config.SwaggerTags;
import br.com.lumilivre.api.dto.settings.SettingsRequest;
import br.com.lumilivre.api.dto.settings.SettingsResponse;
import br.com.lumilivre.api.service.SettingsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
@Tag(name = SwaggerTags.SETTINGS)
public class SettingsController {

    private final SettingsService settingsService;

    @GetMapping
    @Operation(operationId = "settings.get")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SettingsResponse> get() {
        return ResponseEntity.ok(settingsService.getSettingsView());
    }

    @PutMapping
    @Operation(operationId = "settings.update")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SettingsResponse> update(@Valid @RequestBody SettingsRequest request) {
        return ResponseEntity.ok(settingsService.update(request));
    }
}
