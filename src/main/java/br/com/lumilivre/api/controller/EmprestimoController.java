package br.com.lumilivre.api.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
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

import br.com.lumilivre.api.data.EmprestimoDTO;
import br.com.lumilivre.api.enums.StatusEmprestimo;
import br.com.lumilivre.api.model.AlunoModel;
import br.com.lumilivre.api.model.EmprestimoModel;
import br.com.lumilivre.api.model.ExemplarModel;
import br.com.lumilivre.api.model.ResponseModel;
import br.com.lumilivre.api.service.EmprestimoService;

@RestController
@RequestMapping("/emprestimos")
@CrossOrigin(origins = "*", maxAge = 3600, allowCredentials = "false")
public class EmprestimoController {

    @Autowired
    private EmprestimoService es;


    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @GetMapping("/buscar")
    public ResponseEntity<Page<EmprestimoModel>> buscarPorTexto(
            @RequestParam(required = false) String texto,
            Pageable pageable) {

        Page<EmprestimoModel> emprestimos = es.buscarPorTexto(texto, pageable);

        if (emprestimos.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(emprestimos);
    }

    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @GetMapping("/buscar/avancado")
    public ResponseEntity<Page<EmprestimoModel>> buscarAvancado(
            @RequestParam(required = false) StatusEmprestimo statusEmprestimo,
            @RequestParam(required = false) String tombo,
            @RequestParam(required = false) String livroNome,
            @RequestParam(required = false) String alunoNome,
            @RequestParam(required = false) String dataEmprestimo,
            @RequestParam(required = false) String dataDevolucao,
            Pageable pageable) {

        Page<EmprestimoModel> emprestimos = es.buscarAvancado(
                statusEmprestimo,
                tombo,
                livroNome,
                alunoNome,
                dataEmprestimo,
                dataDevolucao,
                pageable);

        if (emprestimos.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(emprestimos);
    }

    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @GetMapping("buscar/ativos")
    public ResponseEntity<List<EmprestimoModel>> buscarAtivos() {
        return ResponseEntity.ok(es.buscarAtivos());
    }

    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @GetMapping("buscar/atrasados")
    public ResponseEntity<List<EmprestimoModel>> buscarAtrasados() {
        return ResponseEntity.ok(es.buscarAtrasados());
    }

    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")

    @GetMapping("buscar/concluidos")
    public ResponseEntity<List<EmprestimoModel>> buscarConcluidos() {
        return ResponseEntity.ok(es.buscarConcluidos());
    }
    
    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @GetMapping("/aluno/{matricula}")
    public ResponseEntity<?> listarEmprestimos(@PathVariable String matricula) {
        return ResponseEntity.ok(es.listarEmprestimosAluno(matricula));
    }

    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @GetMapping("/aluno/{matricula}/historico")
    public ResponseEntity<?> historicoEmprestimos(@PathVariable String matricula) {
        return ResponseEntity.ok(es.listarHistorico(matricula));
    }

    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @PostMapping("/cadastrar")
    public ResponseEntity<?> cadastrar(@RequestBody EmprestimoDTO dto) {
        return es.cadastrar(dto);
    }

    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @PutMapping("/atualizar/{id}")
    public ResponseEntity<?> atualizar(@PathVariable Integer id, @RequestBody EmprestimoDTO dto) {
        dto.setId(id);
        return es.atualizar(dto);
    }
    
    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @PutMapping("/concluir/{id}")
    public ResponseEntity<?> concluirEmprestimo(@PathVariable Integer id) {
        return es.concluirEmprestimo(id);
    }

    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @DeleteMapping("/excluir/{id}")
    public ResponseEntity<ResponseModel> excluir(@PathVariable Integer id) {
        return es.excluir(id);
    }

}
