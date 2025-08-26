package br.com.lumilivre.api.controller;

import java.util.List;

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

import br.com.lumilivre.api.enums.Turno;
import br.com.lumilivre.api.model.CursoModel;
import br.com.lumilivre.api.model.ResponseModel;
import br.com.lumilivre.api.service.CursoService;

@RestController
@RequestMapping("/cursos")
@CrossOrigin(origins = "*", maxAge = 3600, allowCredentials = "false")
public class CursoController {

    @Autowired
    private CursoService cs;

    public CursoController(CursoService CursoService) {
        this.cs = CursoService;
    }

    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @GetMapping("/buscar/todos")
    public List<CursoModel> buscar() {
        return cs.buscar();
    }

    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @GetMapping("/buscar/{turno}")
    public ResponseEntity<?> listarPorTurno(@PathVariable Turno turno) {
        return cs.listarPorTurno(turno);
    }

    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @GetMapping("/buscar")
    public ResponseEntity<Page<CursoModel>> buscarPorTexto(
            @RequestParam(required = false) String texto,
            Pageable pageable) {
        Page<CursoModel> cursos = cs.buscarPorTexto(texto, pageable);
        if (cursos.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(cursos);
    }

    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @GetMapping("/buscar/avancado")
    public ResponseEntity<Page<CursoModel>> buscarAvancado(
            @RequestParam(required = false) String nome,
            @RequestParam(required = false) String turno,
            @RequestParam(required = false) String modulo,
            Pageable pageable) {
        Page<CursoModel> cursos = cs.buscarAvancado(nome, turno, modulo, pageable);
        if (cursos.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(cursos);
    }

    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @PostMapping("/cadastrar")
    public ResponseEntity<?> cadastrar(@RequestBody CursoModel cm) {
        return cs.cadastrar(cm);
    }

    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @PutMapping("atualizar/{id}")
    public ResponseEntity<?> atualizar(@PathVariable Integer id, @RequestBody CursoModel cm) {
        cm.setId(id);
        return cs.atualizar(cm);
    }

}
