package br.com.lumilivre.api.service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.lumilivre.api.data.EmprestimoDTO;
import br.com.lumilivre.api.data.EmprestimoResponseDTO;
import br.com.lumilivre.api.data.ListaEmprestimoDTO;
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

    private static final int LIMITE_EMPRESTIMOS_ATIVOS = 3;

    @Autowired
    private AlunoRepository alunoRepository;

    @Autowired
    private ExemplarRepository exemplarRepository;

    @Autowired
    private EmprestimoRepository emprestimoRepository;

    @Autowired
    private EmailService emailService;

    public Page<ListaEmprestimoDTO> buscarEmprestimoParaListaAdmin(Pageable pageable) {
        return emprestimoRepository.findEmprestimoParaListaAdmin(pageable);
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
            Pageable pageable) {

        LocalDateTime dataEmprestimoDT = null;
        if (dataEmprestimo != null && !dataEmprestimo.isBlank()) {
            dataEmprestimoDT = LocalDate.parse(dataEmprestimo).atStartOfDay();
        }

        LocalDateTime dataDevolucaoDT = null;
        if (dataDevolucao != null && !dataDevolucao.isBlank()) {
            dataDevolucaoDT = LocalDate.parse(dataDevolucao).atStartOfDay();
        }

        return emprestimoRepository.buscarAvancado(
            statusEmprestimo,
            tombo,
            livroNome,
            alunoNome,
            dataEmprestimoDT,
            dataDevolucaoDT,  
            pageable
        );
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

    public List<EmprestimoModel> buscarAtivosEAtrasados() {
        return emprestimoRepository.findByStatusEmprestimoIn(
            List.of(StatusEmprestimo.ATIVO, StatusEmprestimo.ATRASADO)
        );
    }

    public long getContagemEmprestimosAtivosEAtrasados() {
        return emprestimoRepository.countByStatusEmprestimoIn(
            List.of(StatusEmprestimo.ATIVO, StatusEmprestimo.ATRASADO)
        );
    }
    
    public List<EmprestimoModel> buscarApenasAtrasados() {
        return emprestimoRepository.findByStatusEmprestimo(StatusEmprestimo.ATRASADO);
    }

    @Transactional
    public ResponseEntity<ResponseModel> cadastrar(EmprestimoDTO dto) {
        ResponseModel rm = new ResponseModel();

        // Valida datas
        if (dto.getData_emprestimo() == null || dto.getData_devolucao() == null) {
            rm.setMensagem("Datas de empréstimo e devolução são obrigatórias.");
            return ResponseEntity.badRequest().body(rm);
        }
        if (dto.getData_devolucao().isBefore(dto.getData_emprestimo())) {
            rm.setMensagem("A data de devolução não pode ser anterior à data de empréstimo.");
            return ResponseEntity.badRequest().body(rm);
        }

        // Valida aluno
        AlunoModel aluno = alunoRepository.findByMatricula(dto.getAluno_matricula())
                .orElse(null);
        if (aluno == null) {
            rm.setMensagem("Aluno não encontrado.");
            return ResponseEntity.badRequest().body(rm);
        }

        // Valida exemplar
        ExemplarModel exemplar = exemplarRepository.findByTombo(dto.getExemplar_tombo())
                .orElse(null);
        if (exemplar == null) {
            rm.setMensagem("Exemplar não encontrado.");
            return ResponseEntity.badRequest().body(rm);
        }
        if (exemplar.getStatus_livro() != StatusLivro.DISPONIVEL) {
            rm.setMensagem("O exemplar não está disponível para empréstimo.");
            return ResponseEntity.badRequest().body(rm);
        }

        // Valida limite de empréstimos ativos
        long emprestimosAtivosAluno = emprestimoRepository
                .countByAlunoMatriculaAndStatusEmprestimo(aluno.getMatricula(), StatusEmprestimo.ATIVO);
        if (emprestimosAtivosAluno >= LIMITE_EMPRESTIMOS_ATIVOS) {
            rm.setMensagem("O aluno já possui o limite de " + LIMITE_EMPRESTIMOS_ATIVOS + " empréstimos ativos.");
            return ResponseEntity.badRequest().body(rm);
        }

        // Cria empréstimo
        EmprestimoModel emprestimo = new EmprestimoModel();
        emprestimo.setAluno(aluno);
        emprestimo.setExemplar(exemplar);
        emprestimo.setDataEmprestimo(dto.getData_emprestimo());
        emprestimo.setDataDevolucao(dto.getData_devolucao());
        emprestimo.setStatusEmprestimo(StatusEmprestimo.ATIVO);

        exemplar.setStatus_livro(StatusLivro.EMPRESTADO);
        exemplarRepository.save(exemplar);
        emprestimoRepository.save(emprestimo);

        // Envia email ao aluno
        String mensagemEmail = String.format(
                "Olá %s,\n\nSeu empréstimo do livro '%s' foi registrado com sucesso.\n" +
                        "Data de empréstimo: %s\nData de devolução: %s\n\nAtenciosamente,\nBiblioteca LumiLivre",
                aluno.getNome(),
                exemplar.getLivro_isbn().getNome(),
                dto.getData_emprestimo(),
                dto.getData_devolucao()
        );
        emailService.enviarEmail(aluno.getEmail(), "Empréstimo registrado", mensagemEmail);

        rm.setMensagem("Empréstimo cadastrado com sucesso.");
        return ResponseEntity.ok(rm);
    }

    @Transactional
    public ResponseEntity<ResponseModel> atualizar(EmprestimoDTO dto) {
        ResponseModel rm = new ResponseModel();

        EmprestimoModel emprestimo = emprestimoRepository.findById(dto.getId())
                .orElse(null);
        if (emprestimo == null) {
            rm.setMensagem("Empréstimo não encontrado.");
            return ResponseEntity.badRequest().body(rm);
        }
        if (emprestimo.getStatusEmprestimo() == StatusEmprestimo.CONCLUIDO) {
            rm.setMensagem("Este empréstimo já foi concluído e não pode ser alterado.");
            return ResponseEntity.badRequest().body(rm);
        }

        emprestimo.setDataEmprestimo(dto.getData_emprestimo());
        emprestimo.setDataDevolucao(dto.getData_devolucao());
        emprestimoRepository.save(emprestimo);

        rm.setMensagem("Empréstimo alterado com sucesso.");
        return ResponseEntity.ok(rm);
    }

    @Transactional
    public ResponseEntity<ResponseModel> concluirEmprestimo(Integer id) {
        ResponseModel rm = new ResponseModel();

        EmprestimoModel emprestimo = emprestimoRepository.findById(id)
                .orElse(null);
        if (emprestimo == null) {
            rm.setMensagem("Empréstimo não encontrado.");
            return ResponseEntity.badRequest().body(rm);
        }
        if (emprestimo.getStatusEmprestimo() == StatusEmprestimo.CONCLUIDO) {
            rm.setMensagem("Este empréstimo já foi concluído e não pode mais ser alterado.");
            return ResponseEntity.badRequest().body(rm);
        }

        // Calcula penalidade
        if (emprestimo.getDataDevolucao().isBefore(LocalDateTime.now())) {
            long diasAtraso = Duration.between(emprestimo.getDataDevolucao(), LocalDateTime.now()).toDays();
            emprestimo.setPenalidade(calcularPenalidade(diasAtraso));
        }

        emprestimo.setStatusEmprestimo(StatusEmprestimo.CONCLUIDO);

        ExemplarModel exemplar = emprestimo.getExemplar();
        exemplar.setStatus_livro(StatusLivro.DISPONIVEL);
        exemplarRepository.save(exemplar);
        emprestimoRepository.save(emprestimo);

        // Envia email ao aluno
        String mensagemEmail = String.format(
                "Olá %s,\n\nSeu empréstimo do livro '%s' foi concluído com sucesso.\n" +
                        "Status da penalidade: %s\n\nAtenciosamente,\nBiblioteca LumiLivre",
                emprestimo.getAluno().getNome(),
                exemplar.getLivro_isbn().getNome(),
                emprestimo.getPenalidade() != null ? emprestimo.getPenalidade().name() : "Nenhuma"
        );
        emailService.enviarEmail(emprestimo.getAluno().getEmail(), "Empréstimo concluído", mensagemEmail);

        rm.setMensagem("Empréstimo concluído com sucesso.");
        return ResponseEntity.ok(rm);
    }

    private Penalidade calcularPenalidade(long diasAtraso) {
        if (diasAtraso <= 1) return Penalidade.REGISTRO;
        if (diasAtraso <= 5) return Penalidade.ADVERTENCIA;
        if (diasAtraso <= 7) return Penalidade.SUSPENSAO;
        if (diasAtraso <= 10) return Penalidade.BLOQUEIO;
        if (diasAtraso > 90) return Penalidade.BANIMENTO;
        return null; // sem penalidade
    }

    @Transactional
    public ResponseEntity<ResponseModel> excluir(Integer id) {
        ResponseModel rm = new ResponseModel();

        if (id == null || !emprestimoRepository.existsById(id)) {
            rm.setMensagem("Empréstimo não encontrado.");
            return ResponseEntity.badRequest().body(rm);
        }

        emprestimoRepository.deleteById(id);
        rm.setMensagem("Empréstimo removido com sucesso.");
        return ResponseEntity.ok(rm);
    }
}
