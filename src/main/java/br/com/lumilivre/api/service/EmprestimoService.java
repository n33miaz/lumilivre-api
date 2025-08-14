package br.com.lumilivre.api.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import br.com.lumilivre.api.data.EmprestimoDTO;
import br.com.lumilivre.api.enums.StatusEmprestimo;
import br.com.lumilivre.api.enums.StatusLivro;
import br.com.lumilivre.api.model.AlunoModel;
import br.com.lumilivre.api.model.AutorModel;
import br.com.lumilivre.api.model.EmprestimoModel;
import br.com.lumilivre.api.model.ExemplarModel;
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
    
    public Page<EmprestimoModel> buscarPorTexto(String texto, Pageable pageable) {
        if (texto == null || texto.isBlank()) {
            return emprestimoRepository.findAll(pageable);
        }
        return emprestimoRepository.buscarPorTexto(texto, pageable);
    }

    public Page<EmprestimoModel> buscarAvancado(
            StatusEmprestimo statusEmprestimo,
            String tombo,
            String livroNome,
            String alunoNome,
            String dataEmprestimo,
            String dataDevolucao,
            Pageable pageable
    ) {
        return emprestimoRepository.buscarAvancado(
                statusEmprestimo,
                tombo,
                livroNome,
                alunoNome,
                dataEmprestimo,
                dataDevolucao,
                pageable
        );
    }


    public ResponseEntity<?> cadastrar(EmprestimoDTO dto) {

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

        if (dto.getExemplar_tombo() == null || dto.getExemplar_tombo().trim().isEmpty()) {
            rm.setMensagem("O tombo do exemplar é obrigatório.");
            return ResponseEntity.badRequest().body(rm);
        }
        var exemplarOpt = exemplarRepository.findByTombo(dto.getExemplar_tombo());
        if (exemplarOpt.isEmpty()) {
            rm.setMensagem("Exemplar não encontrado para o tombo informado.");
            return ResponseEntity.badRequest().body(rm);
        }

        ExemplarModel exemplar = exemplarOpt.get();

        if (exemplar.getStatus_livro() != StatusLivro.DISPONIVEL) {
            rm.setMensagem("O exemplar não está disponível para empréstimo.");
            return ResponseEntity.badRequest().body(rm);
        }

        if (emprestimoRepository.existsByExemplarTomboAndStatusEmprestimo(dto.getExemplar_tombo(),
                StatusEmprestimo.ATIVO)) {
            rm.setMensagem("Este exemplar já está emprestado em um empréstimo ativo.");
            return ResponseEntity.badRequest().body(rm);
        }

        EmprestimoModel emprestimo = new EmprestimoModel();
        emprestimo.setAluno(alunoOpt.get());
        emprestimo.setExemplar(exemplar);
        emprestimo.setDataEmprestimo(dto.getData_emprestimo());
        emprestimo.setDataDevolucao(dto.getData_devolucao());
        emprestimo.setStatusEmprestimo(status);

        emprestimoRepository.save(emprestimo);

        rm.setMensagem("Empréstimo cadastrado com sucesso.");
        return ResponseEntity.ok(rm);
    }

    public ResponseEntity<?> atualizar(EmprestimoDTO dto) {
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

        emprestimo.setDataEmprestimo(dto.getData_emprestimo());
        emprestimo.setDataDevolucao(dto.getData_devolucao());
        emprestimo.setStatusEmprestimo(status);
        emprestimo.setAluno(alunoOpt.get());
        emprestimo.setExemplar(exemplarOpt.get());

        emprestimoRepository.save(emprestimo);

        rm.setMensagem("Empréstimo alterado com sucesso.");
        return ResponseEntity.ok(rm);
    }

    public ResponseEntity<ResponseModel> excluir(Integer id) {
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

    public List<EmprestimoModel> buscarAtivos() {
        return emprestimoRepository.findByStatusEmprestimoAndDataDevolucaoGreaterThanEqual(
                StatusEmprestimo.ATIVO, LocalDateTime.now());
    }

    public List<EmprestimoModel> buscarAtrasados() {
        return emprestimoRepository.findByStatusEmprestimoAndDataDevolucaoBefore(
                StatusEmprestimo.ATIVO, LocalDateTime.now());
    }

    public List<EmprestimoModel> buscarConcluidos() {
        return emprestimoRepository.findByStatusEmprestimo(StatusEmprestimo.CONCLUIDO);
    }

    public List<EmprestimoModel> listarPorStatusEDataDevolucaoAntes(StatusEmprestimo status, LocalDateTime data) {
        return emprestimoRepository.findByStatusEmprestimoAndDataDevolucaoBefore(status, data);
    }

    public List<EmprestimoModel> listarPorStatusEDataDevolucaoDepoisOuIgual(StatusEmprestimo status,
            LocalDateTime data) {
        return emprestimoRepository.findByStatusEmprestimoAndDataDevolucaoGreaterThanEqual(status, data);
    }

    public List<EmprestimoModel> listarPorAluno(String matricula) {
        return emprestimoRepository.findByAluno_Matricula(matricula);
    }

    public List<EmprestimoModel> listarPorExemplar(String tombo) {
        return emprestimoRepository.findByExemplar_Tombo(tombo);
    }

    public List<EmprestimoModel> listarPorDataEmprestimoIntervalo(LocalDateTime inicio, LocalDateTime fim) {
        return emprestimoRepository.findByDataEmprestimoBetween(inicio, fim);
    }

    public List<EmprestimoModel> listarPorDataEmprestimoAPartirDe(LocalDate dataInicio) {
        LocalDateTime inicio = dataInicio.atStartOfDay();
        return emprestimoRepository.findByDataEmprestimoGreaterThanEqual(inicio);
    }

    public List<EmprestimoModel> listarPorDataDevolucaoIntervalo(LocalDateTime inicio, LocalDateTime fim) {
        return emprestimoRepository.findByDataDevolucaoBetween(inicio, fim);
    }

    public List<EmprestimoModel> listarPorDataEmprestimoIntervalo(LocalDate inicio, LocalDate fim) {
        LocalDateTime start = inicio.atStartOfDay();
        LocalDateTime end = fim.atTime(LocalTime.MAX);
        return listarPorDataEmprestimoIntervalo(start, end);
    }

    public List<EmprestimoModel> listarPorDataDevolucaoIntervalo(LocalDate inicio, LocalDate fim) {
        LocalDateTime start = inicio.atStartOfDay();
        LocalDateTime end = fim.atTime(LocalTime.MAX);
        return listarPorDataDevolucaoIntervalo(start, end);
    }
}
