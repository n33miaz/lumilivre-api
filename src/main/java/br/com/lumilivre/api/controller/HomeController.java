package br.com.lumilivre.api.controller;

import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController

@Tag(name = "10. Home")
@SecurityRequirement(name = "bearerAuth")

public class HomeController {

    @GetMapping("/")
    public String home() {
        return "API Lumilivre rodando com sucesso!";
    }
}
