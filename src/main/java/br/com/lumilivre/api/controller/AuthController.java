package br.com.lumilivre.api.controller;

import java.util.Locale;
import java.util.Map;

import br.com.lumilivre.api.dto.auth.ChangePasswordRequest;
import br.com.lumilivre.api.dto.auth.LoginRequest;
import br.com.lumilivre.api.dto.auth.LoginResponse;
import br.com.lumilivre.api.dto.auth.ResetPasswordTokenRequest;
import br.com.lumilivre.api.service.AppUserService;
import br.com.lumilivre.api.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final AppUserService userService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest req, Locale locale) {
        LoginResponse body = authService.login(req.getUsername(), req.getPassword());
        return ResponseEntity.ok()
                .header("Content-Language", locale.toLanguageTag())
                .body(body);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@RequestBody Map<String, String> payload) {
        authService.solicitarResetSenha(payload.get("email"));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/validate-token/{token}")
    public ResponseEntity<Map<String, Boolean>> validateToken(@PathVariable String token) {
        return ResponseEntity.ok(Map.of("valid", authService.validarTokenReset(token)));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordTokenRequest req) {
        authService.resetPasswordWithToken(req);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest req, Locale locale) {
        userService.changePassword(req);
        return ResponseEntity.noContent()
                .header("Content-Language", locale.toLanguageTag())
                .build();
    }
}
