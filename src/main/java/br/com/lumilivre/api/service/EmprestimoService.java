package br.com.lumilivre.api.service;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import br.com.lumilivre.api.data.EmprestimoDTO;
import br.com.lumilivre.api.enums.StatusEmprestimo;
import br.com.lumilivre.api.model.EmprestimoModel;
import br.com.lumilivre.api.model.ResponseModel;
import br.com.lumilivre.api.repository.AlunoRepository;
import br.com.lumilivre.api.repository.EmprestimoRepository;
import br.com.lumilivre.api.repository.ExemplarRepository;

@Service
public class EmprestimoService {

    @Autowired
    private AlunoRepository alunoRepository;       
    @Autowired
    private ExemplarRepository exemplarRepository; 
	
	@Autowired
	private EmprestimoRepository emprestimoRepository;
	
    @Autowired
    private ResponseModel rm;
	
    public Iterable<EmprestimoModel> listar() {
        return emprestimoRepository.findAll();
    }
    
    public ResponseEntity<?> cadastrar(EmprestimoDTO dto) {

    	// DATA EMPRESTIMO
    	if (dto.getData_emprestimo() == null) {
    	    rm.setMensagem("A data de empréstimo é obrigatória.");
    	    return ResponseEntity.badRequest().body(rm);
    	}

    	if (dto.getData_emprestimo().isBefore(LocalDateTime.now())) {
    	    rm.setMensagem("A data de empréstimo não pode ser anterior à data atual.");
    	    return ResponseEntity.badRequest().body(rm);
    	}
    	
    	// DATA DEVOLUÇÃO
    	if (dto.getData_devolucao() == null) {
    	    rm.setMensagem("A data de empréstimo é obrigatória.");
    	    return ResponseEntity.badRequest().body(rm);
    	}
    	if (dto.getData_devolucao().isBefore(LocalDateTime.now())) {
    	    rm.setMensagem("A data de devolução não pode ser anterior à data atual.");
    	    return ResponseEntity.badRequest().body(rm);
    	}
    	
    	if (dto.getData_emprestimo() != null &&
    		    dto.getData_devolucao().isBefore(dto.getData_emprestimo())) {
    		    
    		    rm.setMensagem("A data de devolução não pode ser anterior à data de empréstimo.");
    		    return ResponseEntity.badRequest().body(rm);
    		}
    	
        if (dto.getStatus_emprestimo() == null || dto.getStatus_emprestimo().trim().isEmpty()) {
            rm.setMensagem("O Status do Empréstimo é obrigatório.");
            return ResponseEntity.badRequest().body(rm);
        }

        StatusEmprestimo status;
        try {
            status = StatusEmprestimo.valueOf(dto.getStatus_emprestimo().toUpperCase());
        } catch (IllegalArgumentException e) {
            rm.setMensagem("Status do empréstimo inválido.");
            return ResponseEntity.badRequest().body(rm);
        }
        
        if (dto.getAluno_matricula() == null || dto.getAluno_matricula().trim().isEmpty()) {
            rm.setMensagem("A matrícula do aluno é obrigatória.");
            return ResponseEntity.badRequest().body(rm);
        }
        var alunoOpt = alunoRepository.findByMatricula(dto.getAluno_matricula());
        if (alunoOpt.isEmpty()) {
            rm.setMensagem("Aluno não encontrado para a matrícula informada.");
            return ResponseEntity.badRequest().body(rm);
        }

        // VALIDAÇÃO TOMBO DO EXEMPLAR
        if (dto.getExemplar_tombo() == null || dto.getExemplar_tombo().trim().isEmpty()) {
            rm.setMensagem("O tombo do exemplar é obrigatório.");
            return ResponseEntity.badRequest().body(rm);
        }
        var exemplarOpt = exemplarRepository.findByTombo(dto.getExemplar_tombo());
        if (exemplarOpt.isEmpty()) {
            rm.setMensagem("Exemplar não encontrado para o tombo informado.");
            return ResponseEntity.badRequest().body(rm);
        }

        EmprestimoModel emprestimo = new EmprestimoModel();
        emprestimo.setAluno(alunoOpt.get());
        emprestimo.setExemplar(exemplarOpt.get());
        emprestimo.setData_emprestimo(dto.getData_emprestimo());
        emprestimo.setData_devolucao(dto.getData_devolucao());
        emprestimo.setStatus_emprestimo(status);

        emprestimoRepository.save(emprestimo);

        rm.setMensagem("Empréstimo cadastrado com sucesso.");
        return ResponseEntity.ok(rm);

    }
    
    public ResponseEntity<?> alterar(EmprestimoDTO dto) {
        if (dto.getId() == null) {
            rm.setMensagem("O ID do empréstimo é obrigatório para alteração.");
            return ResponseEntity.badRequest().body(rm);
        }

        var emprestimoOpt = emprestimoRepository.findById(dto.getId());
        if (emprestimoOpt.isEmpty()) {
            rm.setMensagem("Empréstimo com esse ID não encontrado.");
            return ResponseEntity.badRequest().body(rm);
        }

        EmprestimoModel emprestimo = emprestimoOpt.get();

        // Validar datas
        if (dto.getData_emprestimo() == null) {
            rm.setMensagem("A data de empréstimo é obrigatória.");
            return ResponseEntity.badRequest().body(rm);
        }
        if (dto.getData_emprestimo().isBefore(LocalDateTime.now())) {
            rm.setMensagem("A data de empréstimo não pode ser anterior à data atual.");
            return ResponseEntity.badRequest().body(rm);
        }
        
        if (dto.getData_devolucao() == null) {
            rm.setMensagem("A data de devolução é obrigatória.");
            return ResponseEntity.badRequest().body(rm);
        }
        if (dto.getData_devolucao().isBefore(LocalDateTime.now())) {
            rm.setMensagem("A data de devolução não pode ser anterior à data atual.");
            return ResponseEntity.badRequest().body(rm);
        }
        
        if (dto.getData_devolucao().isBefore(dto.getData_emprestimo())) {
            rm.setMensagem("A data de devolução não pode ser anterior à data de empréstimo.");
            return ResponseEntity.badRequest().body(rm);
        }

        // Validar status
        if (dto.getStatus_emprestimo() == null || dto.getStatus_emprestimo().trim().isEmpty()) {
            rm.setMensagem("O Status do Empréstimo é obrigatório.");
            return ResponseEntity.badRequest().body(rm);
        }

        StatusEmprestimo status;
        try {
            status = StatusEmprestimo.valueOf(dto.getStatus_emprestimo().toUpperCase());
        } catch (IllegalArgumentException e) {
            rm.setMensagem("Status do empréstimo inválido.");
            return ResponseEntity.badRequest().body(rm);
        }

        // Validar aluno
        if (dto.getAluno_matricula() == null || dto.getAluno_matricula().trim().isEmpty()) {
            rm.setMensagem("A matrícula do aluno é obrigatória.");
            return ResponseEntity.badRequest().body(rm);
        }
        var alunoOpt = alunoRepository.findByMatricula(dto.getAluno_matricula());
        if (alunoOpt.isEmpty()) {
            rm.setMensagem("Aluno não encontrado para a matrícula informada.");
            return ResponseEntity.badRequest().body(rm);
        }

        // Validar exemplar
        if (dto.getExemplar_tombo() == null || dto.getExemplar_tombo().trim().isEmpty()) {
            rm.setMensagem("O tombo do exemplar é obrigatório.");
            return ResponseEntity.badRequest().body(rm);
        }
        var exemplarOpt = exemplarRepository.findByTombo(dto.getExemplar_tombo());
        if (exemplarOpt.isEmpty()) {
            rm.setMensagem("Exemplar não encontrado para o tombo informado.");
            return ResponseEntity.badRequest().body(rm);
        }

        // Atualizar dados do empréstimo
        emprestimo.setData_emprestimo(dto.getData_emprestimo());
        emprestimo.setData_devolucao(dto.getData_devolucao());
        emprestimo.setStatus_emprestimo(status);
        emprestimo.setAluno(alunoOpt.get());
        emprestimo.setExemplar(exemplarOpt.get());

        emprestimoRepository.save(emprestimo);

        rm.setMensagem("Empréstimo alterado com sucesso.");
        return ResponseEntity.ok(rm);
    }

    public ResponseEntity<ResponseModel> delete(Integer id) {
        if (id == null) {
            rm.setMensagem("O ID do empréstimo é obrigatório para deletar.");
            return ResponseEntity.badRequest().body(rm);
        }

        if (!emprestimoRepository.existsById(id)) {
            rm.setMensagem("Empréstimo com esse ID não encontrado.");
            return ResponseEntity.badRequest().body(rm);
        }

        emprestimoRepository.deleteById(id);

        rm.setMensagem("Empréstimo removido com sucesso.");
        return ResponseEntity.ok(rm);
    }

        
}
