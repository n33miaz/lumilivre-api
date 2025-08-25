package br.com.lumilivre.api.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import br.com.lumilivre.api.model.AlunoModel;
import br.com.lumilivre.api.model.AutorModel;
import br.com.lumilivre.api.model.ResponseModel;
import br.com.lumilivre.api.service.AlunoService;
import br.com.lumilivre.api.service.AutorService;

@RestController
@RequestMapping("/autores")
@CrossOrigin(origins = "*", maxAge = 3600, allowCredentials = "false")
public class AutorController {

    @Autowired
    private AutorService as;
    
    public AutorController(AutorService AutorService) {
        this.as = AutorService;
    }
    
    @GetMapping("/buscar/todos")
    public Iterable<AutorModel> buscar() {
        return as.buscar();
    }

    @GetMapping("/buscar")
    public ResponseEntity<Page<AutorModel>> buscarPorTexto(
            @RequestParam(required = false) String texto,
            Pageable pageable) {
        Page<AutorModel> autores = as.buscarPorTexto(texto, pageable);
        if (autores.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(autores);
    }

    @GetMapping("/buscar/avancado")
    public ResponseEntity<Page<AutorModel>> buscarAvancado(
            @RequestParam(required = false) String nome,
            @RequestParam(required = false) String pseudonimo,
            @RequestParam(required = false) String nacionalidade,
            Pageable pageable) {
        Page<AutorModel> autores = as.buscarAvancado(nome, pseudonimo, nacionalidade, pageable);
        if (autores.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(autores);
    }

    @PostMapping("/cadastrar")
    public ResponseEntity<?> cadastrar(@RequestBody AutorModel am) {
        return as.cadastrar(am);
    }

    @PutMapping("/atualizar/{codigo}")
    public ResponseEntity<?> atualizar(@PathVariable String codigo, @RequestBody AutorModel am) {
        am.setCodigo(codigo);
        return as.atualizar(am);
    }

    @DeleteMapping("/excluir/{codigo}")
    public ResponseEntity<ResponseModel> excluir(@PathVariable String codigo) {
        return as.excluir(codigo);
    }

}

