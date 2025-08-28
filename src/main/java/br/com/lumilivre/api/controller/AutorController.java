package br.com.lumilivre.api.controller;

import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.lumilivre.api.data.ListaAutorDTO;
import br.com.lumilivre.api.model.AutorModel;
import br.com.lumilivre.api.model.ResponseModel;
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
    
    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @GetMapping("/home")
    public ResponseEntity<Page<ListaAutorDTO>> buscarAutoresAdmin(
            @RequestParam(required = false) String texto,
            Pageable pageable) {

        Page<ListaAutorDTO> autores = as.buscarAutorParaListaAdmin(pageable);

        if (autores.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(autores);
    }

    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
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

    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
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

    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @PostMapping("/cadastrar")
    public ResponseEntity<?> cadastrar(@RequestBody AutorModel am) {
        return as.cadastrar(am);
    }

    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @PutMapping("/atualizar/{codigo}")
    public ResponseEntity<?> atualizar(@PathVariable String codigo, @RequestBody AutorModel am) {
        am.setCodigo(codigo);
        return as.atualizar(am);
    }

    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @DeleteMapping("/excluir/{codigo}")
    public ResponseEntity<ResponseModel> excluir(@PathVariable String codigo) {
        return as.excluir(codigo);
    }

}

