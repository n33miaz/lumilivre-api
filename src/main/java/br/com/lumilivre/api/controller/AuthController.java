package br.com.lumilivre.api.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.lumilivre.api.data.LoginDTO;
import br.com.lumilivre.api.data.LoginResponseDTO;
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

@Tag(name = "1. Autenticação", description = "Endpoints para obter tokens de acesso") // agrupa e ordena no swagger

public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/login")

    @Operation(
        summary = "Realiza o login do usuário", 
        description = "Autentica um usuário com base em 'user' (pode ser matrícula ou email) e 'senha'. Retorna um token JWT e informações básicas do usuário em caso de sucesso.",
        tags = { "1. Autenticação" }
    )
    @ApiResponses
    ({
        @ApiResponse(responseCode = "200", description = "Login bem-sucedido", 
                        content = @Content(mediaType = "application/json", 
                        schema = @Schema(implementation = LoginResponseDTO.class))),
        @ApiResponse(responseCode = "401", description = "Credenciais inválidas (senha incorreta)"),
        @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
    }) 

    public ResponseEntity<?> login(@RequestBody @Valid LoginDTO dto) {
        return authService.login(dto);
    }
}
