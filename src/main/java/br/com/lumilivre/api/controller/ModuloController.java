package br.com.lumilivre.api.controller;

import br.com.lumilivre.api.model.ModuloModel;
import br.com.lumilivre.api.service.ModuloService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/modulos")
public class ModuloController {

    @Autowired
    private ModuloService moduloService;

    @GetMapping
    public List<ModuloModel> listar() {
        return moduloService.listarTodos();
    }

    @PostMapping
    public ModuloModel cadastrar(@RequestBody ModuloModel modulo) {
        return moduloService.cadastrar(modulo);
    }
}