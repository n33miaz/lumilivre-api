package br.com.lumilivre.api.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import br.com.lumilivre.api.dto.LoginDTO;
import br.com.lumilivre.api.dto.LoginResponseDTO;
import br.com.lumilivre.api.dto.MudarSenhaComTokenDTO;
import br.com.lumilivre.api.service.AuthService;
import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/auth")

@Tag(name = "1. Autenticação")

public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/login")

    @Operation(summary = "Realiza o login do usuário", description = "Autentica um usuário com base em 'user' (pode ser matrícula ou email) e 'senha'. Retorna um token JWT e informações básicas do usuário em caso de sucesso.", tags = {
            "1. Autenticação" })
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login bem-sucedido", content = @Content(mediaType = "application/json", schema = @Schema(implementation = LoginResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Credenciais inválidas (senha incorreta)"),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
    })

    public ResponseEntity<?> login(@RequestBody @Valid LoginDTO dto) {
        return authService.login(dto);
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    @PostMapping("/esqueci-senha")

    @Operation(summary = "Solicita a redefinição de senha", description = "Inicia o fluxo de redefinição de senha. O usuário envia seu e-mail. A API sempre retorna sucesso, mas só envia o e-mail se o usuário existir.")
    @ApiResponse(responseCode = "200", description = "Solicitação processada com sucesso")

    public ResponseEntity<Map<String, String>> esqueciSenha(@RequestBody Map<String, String> payload) {
        authService.solicitarResetSenha(payload.get("email"));
        return ResponseEntity.ok(Map.of("mensagem",
                "Se um e-mail correspondente for encontrado, um link para redefinição será enviado."));
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    @GetMapping("/validar-token/{token}")

    @Operation(summary = "Valida um token de redefinição de senha", description = "Verifica se um token de redefinição é válido e não expirou.")
    @ApiResponse(responseCode = "200", description = "Token é válido")
    @ApiResponse(responseCode = "400", description = "Token inválido ou expirado")

    public ResponseEntity<?> validarToken(@PathVariable String token) {
        boolean isValido = authService.validarTokenReset(token);
        if (isValido) {
            return ResponseEntity.ok(Map.of("valido", true));
        }
        return ResponseEntity.badRequest().body(Map.of("valido", false, "mensagem", "Token inválido ou expirado."));
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    @PostMapping("/mudar-senha")

    @Operation(summary = "Muda a senha usando um token", description = "Define uma nova senha para o usuário associado a um token de redefinição válido.")
    @ApiResponse(responseCode = "200", description = "Senha alterada com sucesso")
    @ApiResponse(responseCode = "400", description = "Token inválido ou expirado")

    public ResponseEntity<?> mudarSenha(@RequestBody @Valid MudarSenhaComTokenDTO dto) {
        try {
            authService.mudarSenhaComToken(dto);
            return ResponseEntity.ok(Map.of("mensagem", "Senha alterada com sucesso."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("mensagem", e.getMessage()));
        }
    }
}