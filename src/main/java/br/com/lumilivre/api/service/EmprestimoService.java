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

    public Iterable<EmprestimoModel> listar() {
        return emprestimoRepository.findAll();
    }
    
    public List<EmprestimoResponseDTO> listarEmprestimosAluno(String matricula) {
        return emprestimoRepository.findEmprestimosAtivos(matricula);
    }

    public List<EmprestimoResponseDTO> listarHistorico(String matricula) {
        return emprestimoRepository.findHistoricoEmprestimos(matricula);
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

    // =========================
    // CADASTRAR EMPRÉSTIMO
    // =========================
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

        // 🔹 Validação: livro disponível
        if (exemplar.getStatus_livro() != StatusLivro.DISPONIVEL) {
            rm.setMensagem("O exemplar não está disponível para empréstimo.");
            return ResponseEntity.badRequest().body(rm);
        }

        // 🔹 Validação: aluno não pode ter mais de 3 empréstimos ativos
        long emprestimosAtivosAluno = emprestimoRepository
                .countByAlunoMatriculaAndStatusEmprestimo(dto.getAluno_matricula(), StatusEmprestimo.ATIVO);

        if (emprestimosAtivosAluno >= 3) {
            rm.setMensagem("O aluno já possui o limite de 3 empréstimos ativos.");
            return ResponseEntity.badRequest().body(rm);
        }

        // 🔹 Criação do empréstimo
        EmprestimoModel emprestimo = new EmprestimoModel();
        emprestimo.setAluno(alunoOpt.get());
        emprestimo.setExemplar(exemplar);
        emprestimo.setDataEmprestimo(dto.getData_emprestimo());
        emprestimo.setDataDevolucao(dto.getData_devolucao());
        emprestimo.setStatusEmprestimo(StatusEmprestimo.ATIVO);

        // 🔹 Alterar status do livro para EMPRESTADO
        exemplar.setStatus_livro(StatusLivro.EMPRESTADO);
        exemplarRepository.save(exemplar);

        emprestimoRepository.save(emprestimo);

        rm.setMensagem("Empréstimo cadastrado com sucesso.");
        return ResponseEntity.ok(rm);
    }

    // =========================
    // CONCLUIR EMPRÉSTIMO
    // =========================
    public ResponseEntity<?> concluirEmprestimo(Integer id) {
        var emprestimoOpt = emprestimoRepository.findById(id);
        if (emprestimoOpt.isEmpty()) {
            rm.setMensagem("Empréstimo não encontrado.");
            return ResponseEntity.badRequest().body(rm);
        }

        EmprestimoModel emprestimo = emprestimoOpt.get();

        // 🔹 Se já está concluído, não permitir alterações
        if (emprestimo.getStatusEmprestimo() == StatusEmprestimo.CONCLUIDO) {
            rm.setMensagem("Este empréstimo já foi concluído e não pode mais ser alterado.");
            return ResponseEntity.badRequest().body(rm);
        }

        // 🔹 Se devolveu atrasado → aplica penalidade
        if (emprestimo.getDataDevolucao().isBefore(LocalDateTime.now())) {
            long diasAtraso = java.time.Duration.between(
                    emprestimo.getDataDevolucao(), LocalDateTime.now()
            ).toDays();

            if (diasAtraso <= 1) {
                emprestimo.setPenalidade(Penalidade.REGISTRO);
            } else if (diasAtraso <= 5) {
                emprestimo.setPenalidade(Penalidade.ADVERTENCIA);
            } else if (diasAtraso <= 7) {
                emprestimo.setPenalidade(Penalidade.SUSPENSAO);
            } else if (diasAtraso <= 10) {
                emprestimo.setPenalidade(Penalidade.BLOQUEIO);
                // futuro: registrar data fim do bloqueio
            } else if (diasAtraso > 90) {
                emprestimo.setPenalidade(Penalidade.BANIMENTO);
            }
        }

        // 🔹 Concluir empréstimo
        emprestimo.setStatusEmprestimo(StatusEmprestimo.CONCLUIDO);

        // 🔹 Liberar exemplar
        ExemplarModel exemplar = emprestimo.getExemplar();
        exemplar.setStatus_livro(StatusLivro.DISPONIVEL);
        exemplarRepository.save(exemplar);

        emprestimoRepository.save(emprestimo);

        rm.setMensagem("Empréstimo concluído com sucesso.");
        return ResponseEntity.ok(rm);
    }

    // =========================
    // ATUALIZAR EMPRÉSTIMO
    // =========================
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

        // 🔹 Se já está concluído, não permitir alterações
        if (emprestimo.getStatusEmprestimo() == StatusEmprestimo.CONCLUIDO) {
            rm.setMensagem("Este empréstimo já foi concluído e não pode mais ser alterado.");
            return ResponseEntity.badRequest().body(rm);
        }

        // 🔹 Só altera se ainda estiver em andamento
        emprestimo.setDataEmprestimo(dto.getData_emprestimo());
        emprestimo.setDataDevolucao(dto.getData_devolucao());

        emprestimoRepository.save(emprestimo);

        rm.setMensagem("Empréstimo alterado com sucesso.");
        return ResponseEntity.ok(rm);
    }


    // =========================
    // EXCLUIR EMPRÉSTIMO
    // =========================
    public ResponseEntity<ResponseModel> excluir(Integer id) {
        if (id == null || !emprestimoRepository.existsById(id)) {
            rm.setMensagem("Empréstimo não encontrado.");
            return ResponseEntity.badRequest().body(rm);
        }

        emprestimoRepository.deleteById(id);

        rm.setMensagem("Empréstimo removido com sucesso.");
        return ResponseEntity.ok(rm);
    }

    // =========================
    // LISTAGENS POR STATUS
    // =========================
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
