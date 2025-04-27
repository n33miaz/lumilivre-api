package br.com.lumilivre.api.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.lumilivre.api.data.AlunoRequestDTO;
import br.com.lumilivre.api.model.AlunoModel;
import br.com.lumilivre.api.service.AlunoService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController
@RequestMapping("/lumilivre/alunos")
@CrossOrigin(origins = "*", maxAge = 3600, allowCredentials = "false")
public class AlunoController {

    @Autowired
    private AlunoService as;

    @PostMapping("/cadastrar")
    public ResponseEntity<?> cadastrar(@RequestBody AlunoRequestDTO alunoRequestDTO) {
        // Convertendo o AlunoRequestDTO para AlunoModel
        AlunoModel alunoModel = new AlunoModel();
        alunoModel.setMatricula(alunoRequestDTO.getMatricula());
        alunoModel.setNome(alunoRequestDTO.getNome());
        alunoModel.setSobrenome(alunoRequestDTO.getSobrenome());
        alunoModel.setCpf(alunoRequestDTO.getCpf());
        alunoModel.setDataNascimento(alunoRequestDTO.getDataNascimento());
        alunoModel.setCelular(alunoRequestDTO.getCelular());
        alunoModel.setEmail(alunoRequestDTO.getEmail());

        // Passando o alunoModel e o CEP para o serviço
        return as.cadastrarAluno(alunoModel, alunoRequestDTO.getCep());
    }

    @GetMapping("/listar")
    public Iterable<AlunoModel> listar() {
        return as.listar();
    }
    
    @DeleteMapping("/remover/{matricula}")
    public ResponseEntity<?> remover(@PathVariable String matricula) {
        return as.delete(matricula);
    }
}
