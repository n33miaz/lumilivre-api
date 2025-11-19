package br.com.lumilivre.api.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import br.com.lumilivre.api.dto.LoginDTO;
import br.com.lumilivre.api.dto.LoginResponseDTO;
import br.com.lumilivre.api.dto.MudarSenhaComTokenDTO;
import br.com.lumilivre.api.model.ResponseModel;
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
    @Operation(summary = "Realiza o login do usuário", description = "Autentica via matrícula ou email.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login bem-sucedido", content = @Content(schema = @Schema(implementation = LoginResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Credenciais inválidas"),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
    })
    public ResponseEntity<LoginResponseDTO> login(@RequestBody @Valid LoginDTO dto) {
        LoginResponseDTO response = authService.login(dto);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/esqueci-senha")
    @Operation(summary = "Solicita a redefinição de senha")
    @ApiResponse(responseCode = "200", description = "Solicitação processada")
    public ResponseEntity<ResponseModel> esqueciSenha(@RequestBody Map<String, String> payload) {
        authService.solicitarResetSenha(payload.get("email"));
        return ResponseEntity.ok(new ResponseModel(
                "Se um e-mail correspondente for encontrado, um link para redefinição será enviado."));
    }

    @GetMapping("/validar-token/{token}")
    @Operation(summary = "Valida um token de redefinição de senha")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token é válido"),
            @ApiResponse(responseCode = "400", description = "Token inválido ou expirado")
    })
    public ResponseEntity<Map<String, Boolean>> validarToken(@PathVariable String token) {
        boolean isValido = authService.validarTokenReset(token);
        if (isValido) {
            return ResponseEntity.ok(Map.of("valido", true));
        }
        return ResponseEntity.badRequest().body(Map.of("valido", false));
    }

    @PostMapping("/mudar-senha")
    @Operation(summary = "Muda a senha usando um token")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Senha alterada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Token inválido ou expirado")
    })
    public ResponseEntity<ResponseModel> mudarSenha(@RequestBody @Valid MudarSenhaComTokenDTO dto) {
        authService.mudarSenhaComToken(dto);
        return ResponseEntity.ok(new ResponseModel("Senha alterada com sucesso."));
    }
}