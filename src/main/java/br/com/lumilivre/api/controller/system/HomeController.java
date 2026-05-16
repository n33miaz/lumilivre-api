package br.com.lumilivre.api.controller.system;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "0. Home")
@SecurityRequirement(name = "bearerAuth")
public class HomeController {

    @GetMapping("/")
    @Operation(summary = "Health check", description = "Returns a simple message indicating the API is online.")
    public String home() {
        return "API Lumilivre rodando com sucesso!";
    }
}
