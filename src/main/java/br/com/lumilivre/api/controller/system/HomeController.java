package br.com.lumilivre.api.controller.system;

import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
// @Tag(name = "0. Home")
@SecurityRequirement(name = "bearerAuth")
public class HomeController {

    @GetMapping("/")
    @Operation(summary = "Verifica o status da API", description = "Retorna uma mensagem simples indicando que a API está online.")
    public String home() {
        return "API Lumilivre rodando com sucesso!";
    }
}