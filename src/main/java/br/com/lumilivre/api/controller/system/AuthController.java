package br.com.lumilivre.api.controller.system;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import br.com.lumilivre.api.dto.auth.LoginRequest;
import br.com.lumilivre.api.dto.auth.LoginResponse;
import br.com.lumilivre.api.dto.auth.MudarSenhaTokenRequest;
import br.com.lumilivre.api.dto.comum.ApiResponse;
import br.com.lumilivre.api.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/auth")
// @Tag(name = "1. Autenticação")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @Operation(summary = "Realiza o login do usuário", description = "Autentica o usuário via matrícula (Aluno) ou email (Admin/Bibliotecário) e retorna o token JWT.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Login bem-sucedido", content = @Content(schema = @Schema(implementation = LoginResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Credenciais inválidas"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Usuário não encontrado")
    })
    public ResponseEntity<LoginResponse> login(@RequestBody @Valid LoginRequest dto) {
        LoginResponse response = authService.login(dto);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/esqueci-senha")
    @Operation(summary = "Solicita a redefinição de senha", description = "Envia um e-mail com link de recuperação caso o e-mail informado exista na base de dados. O payload deve conter a chave 'email'.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Solicitação processada (independente se o e-mail existe ou não, por segurança)")
    public ResponseEntity<ApiResponse<Void>> esqueciSenha(@RequestBody Map<String, String> payload) {
        authService.solicitarResetSenha(payload.get("email"));

        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Se um e-mail correspondente for encontrado, um link para redefinição será enviado.",
                null));
    }

    @GetMapping("/validar-token/{token}")
    @Operation(summary = "Valida um token de redefinição de senha", description = "Verifica se o token de recuperação existe e se ainda não expirou.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Token é válido"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Token inválido ou expirado")
    })
    public ResponseEntity<Map<String, Boolean>> validarToken(@PathVariable String token) {
        boolean isValido = authService.validarTokenReset(token);
        if (isValido) {
            return ResponseEntity.ok(Map.of("valido", true));
        }
        return ResponseEntity.badRequest().body(Map.of("valido", false));
    }

    @PostMapping("/mudar-senha")
    @Operation(summary = "Muda a senha usando um token", description = "Atualiza a senha do usuário vinculado ao token de recuperação validado.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Senha alterada com sucesso"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Token inválido, expirado ou dados incorretos")
    })
    public ResponseEntity<ApiResponse<Void>> mudarSenha(@RequestBody @Valid MudarSenhaTokenRequest dto) {
        authService.mudarSenhaComToken(dto);

        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Senha alterada com sucesso.",
                null));
    }
}