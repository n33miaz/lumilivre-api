package br.com.lumilivre.api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
public class HomeController {

    @GetMapping("/")
    public String home() {
        return "API Lumilivre rodando com sucesso!";
    }
}
