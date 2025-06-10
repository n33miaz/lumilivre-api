package br.com.lumilivre.api.controller;

import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import br.com.lumilivre.api.data.AlunoLoginDTO;
import br.com.lumilivre.api.model.AlunoModel;
import br.com.lumilivre.api.repository.AlunoRepository;

@RestController
@RequestMapping("/lumilivre/alunos")
@CrossOrigin(origins = "*", maxAge = 3600, allowCredentials = "false")
public class AlunoLoginController {
    
@PostMapping("/login")
public ResponseEntity<?> loginAluno(@RequestBody AlunoLoginDTO request) {
    Optional<AlunoModel> alunoOpt = AlunoRepository.findByMatricula(request.getMatricula());

    if (alunoOpt.isPresent()) {
        AlunoModel aluno = alunoOpt.get();
        if (aluno.getCpf().equals(request.getCpf())) {
            return ResponseEntity.ok("Login realizado com sucesso!");
        }
    }

    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Matrícula ou CPF inválido.");
}
}
