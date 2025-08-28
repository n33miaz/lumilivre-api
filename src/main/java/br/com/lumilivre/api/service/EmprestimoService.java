package br.com.lumilivre.api.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import br.com.lumilivre.api.data.EmprestimoDTO;
import br.com.lumilivre.api.data.EmprestimoResponseDTO;
import br.com.lumilivre.api.enums.Penalidade;
import br.com.lumilivre.api.enums.StatusEmprestimo;
import br.com.lumilivre.api.enums.StatusLivro;
import br.com.lumilivre.api.model.AlunoModel;
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
            Pageable pageable) {
        return emprestimoRepository.buscarAvancado(
                statusEmprestimo,
                tombo,
                livroNome,
                alunoNome,
                dataEmprestimo,
                dataDevolucao,
                pageable);
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

    public Iterable<EmprestimoModel> listarDisponiveis() {
        return emprestimoRepository.findByStatusEmprestimo(StatusEmprestimo.ATIVO);
    }

    public List<EmprestimoResponseDTO> listarEmprestimosAluno(String matricula) {
        return emprestimoRepository.findEmprestimosAtivos(matricula);
    }

    public List<EmprestimoResponseDTO> listarHistorico(String matricula) {
        return emprestimoRepository.findHistoricoEmprestimos(matricula);
    }


    public ResponseEntity<?> cadastrar(EmprestimoDTO dto) {

        if (dto.getData_emprestimo() == null || dto.getData_devolucao() == null) {
            rm.setMensagem("Datas de empréstimo e devolução são obrigatórias.");
            return ResponseEntity.badRequest().body(rm);
        }

        if (dto.getData_devolucao().isBefore(dto.getData_emprestimo())) {
            rm.setMensagem("A data de devolução não pode ser anterior à data de empréstimo.");
            return ResponseEntity.badRequest().body(rm);
        }

        var alunoOpt = alunoRepository.findByMatricula(dto.getAluno_matricula());
        if (alunoOpt.isEmpty()) {
            rm.setMensagem("Aluno não encontrado.");
            return ResponseEntity.badRequest().body(rm);
        }

        var exemplarOpt = exemplarRepository.findByTombo(dto.getExemplar_tombo());
        if (exemplarOpt.isEmpty()) {
            rm.setMensagem("Exemplar não encontrado.");
            return ResponseEntity.badRequest().body(rm);
        }

        ExemplarModel exemplar = exemplarOpt.get();

        if (exemplar.getStatus_livro() != StatusLivro.DISPONIVEL) {
            rm.setMensagem("O exemplar não está disponível para empréstimo.");
            return ResponseEntity.badRequest().body(rm);
        }

        long emprestimosAtivosAluno = emprestimoRepository
                .countByAlunoMatriculaAndStatusEmprestimo(dto.getAluno_matricula(), StatusEmprestimo.ATIVO);

        if (emprestimosAtivosAluno >= 3) {
            rm.setMensagem("O aluno já possui o limite de 3 empréstimos ativos.");
            return ResponseEntity.badRequest().body(rm);
        }

        EmprestimoModel emprestimo = new EmprestimoModel();
        emprestimo.setAluno(alunoOpt.get());
        emprestimo.setExemplar(exemplar);
        emprestimo.setDataEmprestimo(dto.getData_emprestimo());
        emprestimo.setDataDevolucao(dto.getData_devolucao());
        emprestimo.setStatusEmprestimo(StatusEmprestimo.ATIVO);

        exemplar.setStatus_livro(StatusLivro.EMPRESTADO);
        exemplarRepository.save(exemplar);

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
            rm.setMensagem("Empréstimo não encontrado.");
            return ResponseEntity.badRequest().body(rm);
        }

        EmprestimoModel emprestimo = emprestimoOpt.get();

        if (emprestimo.getStatusEmprestimo() == StatusEmprestimo.CONCLUIDO) {
            rm.setMensagem("Este empréstimo já foi concluído e não pode mais ser alterado.");
            return ResponseEntity.badRequest().body(rm);
        }

        emprestimo.setDataEmprestimo(dto.getData_emprestimo());
        emprestimo.setDataDevolucao(dto.getData_devolucao());

        emprestimoRepository.save(emprestimo);

        rm.setMensagem("Empréstimo alterado com sucesso.");
        return ResponseEntity.ok(rm);
    }

    public ResponseEntity<?> concluirEmprestimo(Integer id) {
        var emprestimoOpt = emprestimoRepository.findById(id);
        if (emprestimoOpt.isEmpty()) {
            rm.setMensagem("Empréstimo não encontrado.");
            return ResponseEntity.badRequest().body(rm);
        }

        EmprestimoModel emprestimo = emprestimoOpt.get();

        if (emprestimo.getStatusEmprestimo() == StatusEmprestimo.CONCLUIDO) {
            rm.setMensagem("Este empréstimo já foi concluído e não pode mais ser alterado.");
            return ResponseEntity.badRequest().body(rm);
        }

        if (emprestimo.getDataDevolucao().isBefore(LocalDateTime.now())) {
            long diasAtraso = java.time.Duration.between(
                    emprestimo.getDataDevolucao(), LocalDateTime.now()).toDays();

            if (diasAtraso <= 1) {
                emprestimo.setPenalidade(Penalidade.REGISTRO);
            } else if (diasAtraso <= 5) {
                emprestimo.setPenalidade(Penalidade.ADVERTENCIA);
            } else if (diasAtraso <= 7) {
                emprestimo.setPenalidade(Penalidade.SUSPENSAO);
            } else if (diasAtraso <= 10) {
                emprestimo.setPenalidade(Penalidade.BLOQUEIO);
            } else if (diasAtraso > 90) {
                emprestimo.setPenalidade(Penalidade.BANIMENTO);
            }
        }

        emprestimo.setStatusEmprestimo(StatusEmprestimo.CONCLUIDO);

        ExemplarModel exemplar = emprestimo.getExemplar();
        exemplar.setStatus_livro(StatusLivro.DISPONIVEL);
        exemplarRepository.save(exemplar);

        emprestimoRepository.save(emprestimo);

        rm.setMensagem("Empréstimo concluído com sucesso.");
        return ResponseEntity.ok(rm);
    }

    public ResponseEntity<ResponseModel> excluir(Integer id) {
        if (id == null || !emprestimoRepository.existsById(id)) {
            rm.setMensagem("Empréstimo não encontrado.");
            return ResponseEntity.badRequest().body(rm);
        }

        emprestimoRepository.deleteById(id);

        rm.setMensagem("Empréstimo removido com sucesso.");
        return ResponseEntity.ok(rm);
    }
}
