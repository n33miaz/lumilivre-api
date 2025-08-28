package br.com.lumilivre.api.controller;

import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
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

import br.com.lumilivre.api.data.AlunoDTO;
import br.com.lumilivre.api.data.ListaAlunoDTO;
import br.com.lumilivre.api.model.AlunoModel;
import br.com.lumilivre.api.model.ResponseModel;
import br.com.lumilivre.api.service.AlunoService;

@RestController
@RequestMapping("/alunos")
@CrossOrigin(origins = "*", maxAge = 3600, allowCredentials = "false")
public class AlunoController {

    @Autowired
    private AlunoService as;

    public AlunoController(AlunoService AlunoService) {
        this.as = AlunoService;
    }

    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @GetMapping("/home")
    public ResponseEntity<Page<ListaAlunoDTO>> buscarAlunosAdmin(
            @RequestParam(required = false) String texto,
            Pageable pageable) {

        Page<ListaAlunoDTO> alunos = as.buscarAlunosParaListaAdmin(pageable);

        if (alunos.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(alunos);
    }

    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @GetMapping("/buscar")
    public ResponseEntity<Page<AlunoModel>> buscarPorTexto(
            @RequestParam(required = false) String texto,
            Pageable pageable) {
        Page<AlunoModel> alunos = as.buscarPorTexto(texto, pageable);
        if (alunos.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(alunos);
    }

    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @GetMapping("/buscar/avancado")
    public ResponseEntity<Page<AlunoModel>> buscarAvancado(
            @RequestParam(required = false) String nome,
            @RequestParam(required = false) String matricula,
            @RequestParam(required = false) LocalDate dataNascimento,
            @RequestParam(required = false) String cursoNome,
            Pageable pageable) {
        Page<AlunoModel> alunos = as.buscarAvancado(nome, matricula, dataNascimento, cursoNome, pageable);
        if (alunos.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(alunos);
    }


    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @PostMapping("/cadastrar")
    public ResponseEntity<?> cadastrar(@RequestBody AlunoDTO alunoDTO) {
        return as.cadastrar(alunoDTO);
    }


    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO','ALUNO')")
    @PutMapping("/atualizar/{id}")
    public ResponseEntity<?> atualizar(@PathVariable String id, @RequestBody AlunoDTO alunoDTO, Authentication authentication) {
        String usuarioLogado = authentication.getName(); 

        if (authentication.getAuthorities().stream().anyMatch(r -> r.getAuthority().equals("ROLE_ALUNO"))) {
            if (!usuarioLogado.equals(id)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Aluno só pode atualizar seu próprio cadastro.");
            }
        }

        return as.atualizar(id, alunoDTO);
    }


    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/excluir/{id}")
    public ResponseEntity<ResponseModel> excluir(@PathVariable String id) {
        return as.excluir(id);
    }
}
