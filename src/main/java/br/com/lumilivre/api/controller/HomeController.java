package br.com.lumilivre.api.controller;

import br.com.lumilivre.api.config.SwaggerTags;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = SwaggerTags.SYSTEM)
public class HomeController {

    @GetMapping("/")
    @Operation(operationId = "system.home")
    public String home() {
        return "API Lumilivre rodando com sucesso!";
    }
}
