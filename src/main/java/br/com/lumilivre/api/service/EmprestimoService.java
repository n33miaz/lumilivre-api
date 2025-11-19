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

import br.com.lumilivre.api.dto.AlunoRankingDTO;
import br.com.lumilivre.api.dto.EmprestimoDTO;
import br.com.lumilivre.api.dto.EmprestimoResponseDTO;
import br.com.lumilivre.api.dto.ListaEmprestimoDTO;
import br.com.lumilivre.api.dto.ListaEmprestimoDashboardDTO;
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

    // ================ MÉTODOS DE BUSCA ================

    public Page<ListaEmprestimoDTO> buscarEmprestimoParaListaAdmin(Pageable pageable) {
        return emprestimoRepository.findEmprestimoParaListaAdmin(pageable);
    }

    public Page<ListaEmprestimoDTO> buscarPorTexto(String texto, Pageable pageable) {
        if (texto == null || texto.isBlank()) {
            return buscarAvancado(null, null, null, null, null, null, pageable);
        }
        return emprestimoRepository.buscarPorTexto(texto, pageable);
    }

    public List<EmprestimoResponseDTO> listarEmprestimosAluno(String matricula) {
        return emprestimoRepository.findEmprestimosAtivos(matricula);
    }

    public List<EmprestimoResponseDTO> listarHistorico(String matricula) {
        return emprestimoRepository.findHistoricoEmprestimos(matricula);
    }

    public List<ListaEmprestimoDashboardDTO> listarEmprestimosAtivosEAtrasados() {
        return emprestimoRepository.findEmprestimosAtivosEAtrasados();
    }

    public List<EmprestimoModel> buscarAtivosEAtrasados() {
        return emprestimoRepository.findByStatusEmprestimoIn(
                List.of(StatusEmprestimo.ATIVO, StatusEmprestimo.ATRASADO));
    }

    public long getContagemEmprestimosAtivosEAtrasados() {
        return emprestimoRepository.countByStatusEmprestimoIn(
                List.of(StatusEmprestimo.ATIVO, StatusEmprestimo.ATRASADO));
    }

    public List<EmprestimoModel> buscarApenasAtrasados() {
        return emprestimoRepository.findByStatusEmprestimo(StatusEmprestimo.ATRASADO);
    }

    public Page<ListaEmprestimoDTO> buscarAvancado(
            StatusEmprestimo statusEmprestimo,
            String tombo,
            String livroNome,
            String alunoNomeCompleto,
            String dataEmprestimo,
            String dataDevolucao,
            Pageable pageable) {

        String tomboFiltro = (tombo != null && !tombo.isBlank()) ? "%" + tombo + "%" : null;
        String livroNomeFiltro = (livroNome != null && !livroNome.isBlank()) ? "%" + livroNome + "%" : null;
        String alunoNomeFiltro = (alunoNomeCompleto != null && !alunoNomeCompleto.isBlank())
                ? "%" + alunoNomeCompleto + "%"
                : null;

        LocalDateTime dataEmprestimoInicio = null;
        if (dataEmprestimo != null && !dataEmprestimo.isBlank()) {
            dataEmprestimoInicio = LocalDate.parse(dataEmprestimo).atStartOfDay();
        }

        LocalDateTime dataDevolucaoFim = null;
        if (dataDevolucao != null && !dataDevolucao.isBlank()) {
            dataDevolucaoFim = LocalDate.parse(dataDevolucao).atTime(23, 59, 59);
        }

        return emprestimoRepository.buscarAvancado(
                statusEmprestimo,
                tomboFiltro,
                livroNomeFiltro,
                alunoNomeFiltro,
                dataEmprestimoInicio,
                null,
                null,
                dataDevolucaoFim,
                pageable);
    }

    // MÉTODOS DE CADASTRO, ATUALIZAÇÃO E EXCLUSÃO (SEM ALTERAÇÕES)

    @Transactional
    public ResponseEntity<ResponseModel> cadastrar(EmprestimoDTO dto) {
        ResponseModel rm = new ResponseModel();

        if (dto.getData_emprestimo() == null || dto.getData_devolucao() == null) {
            rm.setMensagem("Datas de empréstimo e devolução são obrigatórias.");
            return ResponseEntity.badRequest().body(rm);
        }
        if (dto.getData_devolucao().isBefore(dto.getData_emprestimo())) {
            rm.setMensagem("A data de devolução não pode ser anterior à data de empréstimo.");
            return ResponseEntity.badRequest().body(rm);
        }

        AlunoModel aluno = alunoRepository.findByMatricula(dto.getAluno_matricula())
                .orElseThrow(() -> new IllegalArgumentException("Aluno não encontrado."));

        if (aluno.getPenalidade() != null) {
            rm.setMensagem("O aluno possui penalidade ativa e não pode realizar empréstimos.");
            return ResponseEntity.badRequest().body(rm);
        }

        ExemplarModel exemplar = exemplarRepository.findByTombo(dto.getExemplar_tombo())
                .orElseThrow(() -> new IllegalArgumentException("Exemplar não encontrado."));

        if (exemplar.getStatus_livro() != StatusLivro.DISPONIVEL) {
            rm.setMensagem("O exemplar não está disponível para empréstimo.");
            return ResponseEntity.badRequest().body(rm);
        }

        long emprestimosAtivosAluno = emprestimoRepository
                .countByAlunoMatriculaAndStatusEmprestimo(aluno.getMatricula(), StatusEmprestimo.ATIVO);
        if (emprestimosAtivosAluno >= LIMITE_EMPRESTIMOS_ATIVOS) {
            rm.setMensagem("O aluno já possui o limite de " + LIMITE_EMPRESTIMOS_ATIVOS + " empréstimos ativos.");
            return ResponseEntity.badRequest().body(rm);
        }

        EmprestimoModel emprestimo = new EmprestimoModel();
        emprestimo.setAluno(aluno);
        emprestimo.setExemplar(exemplar);
        emprestimo.setDataEmprestimo(dto.getData_emprestimo());
        emprestimo.setDataDevolucao(dto.getData_devolucao());
        emprestimo.setStatusEmprestimo(StatusEmprestimo.ATIVO);

        exemplar.setStatus_livro(StatusLivro.EMPRESTADO);
        exemplarRepository.save(exemplar);

        aluno.incrementarEmprestimos();
        alunoRepository.save(aluno);

        emprestimoRepository.save(emprestimo);

        String mensagemEmail = String.format(
                "Olá %s,\\n\\nSeu empréstimo do livro '%s' foi registrado com sucesso.\\n" +
                        "Data de empréstimo: %s\\nData de devolução: %s\\n\\nAtenciosamente,\\nBiblioteca LumiLivre",
                aluno.getNomeCompleto(),
                exemplar.getLivro().getNome(),
                dto.getData_emprestimo(),
                dto.getData_devolucao());
        emailService.enviarEmail(aluno.getEmail(), "Empréstimo registrado", mensagemEmail);

        rm.setMensagem("Empréstimo cadastrado com sucesso.");
        return ResponseEntity.ok(rm);
    }

    @Transactional
    public ResponseEntity<ResponseModel> atualizar(EmprestimoDTO dto) {
        ResponseModel rm = new ResponseModel();
        EmprestimoModel emprestimo = emprestimoRepository.findById(dto.getId())
                .orElseThrow(() -> new IllegalArgumentException("Empréstimo não encontrado."));

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
                .orElseThrow(() -> new IllegalArgumentException("Empréstimo não encontrado."));

        if (emprestimo.getStatusEmprestimo() == StatusEmprestimo.CONCLUIDO) {
            rm.setMensagem("Este empréstimo já foi concluído e não pode mais ser alterado.");
            return ResponseEntity.badRequest().body(rm);
        }

        LocalDateTime agora = LocalDateTime.now();
        AlunoModel aluno = emprestimo.getAluno();

        if (emprestimo.getDataDevolucao().isBefore(agora)) {
            Penalidade penalidade = calcularPenalidade(Duration.between(emprestimo.getDataDevolucao(), agora).toDays());
            emprestimo.setPenalidade(penalidade);

            if (aluno.getPenalidade() == null || penalidadeMaisGrave(penalidade, aluno.getPenalidade())) {
                aluno.setPenalidade(penalidade);
                aluno.setPenalidadeExpiraEm(agora.plusDays(7));
                alunoRepository.save(aluno);
            }
        }

        emprestimo.setStatusEmprestimo(StatusEmprestimo.CONCLUIDO);
        ExemplarModel exemplar = emprestimo.getExemplar();
        exemplar.setStatus_livro(StatusLivro.DISPONIVEL);

        exemplarRepository.save(exemplar);
        emprestimoRepository.save(emprestimo);

        String mensagemEmail = String.format(
                "Olá %s,\\n\\nSeu empréstimo do livro '%s' foi concluído.\\n" +
                        "Status da penalidade: %s\\n\\nAtenciosamente,\\nBiblioteca LumiLivre",
                aluno.getNomeCompleto(),
                exemplar.getLivro().getNome(),
                emprestimo.getPenalidade() != null ? emprestimo.getPenalidade().name() : "Nenhuma");
        emailService.enviarEmail(aluno.getEmail(), "Empréstimo concluído", mensagemEmail);

        rm.setMensagem("Empréstimo concluído com sucesso.");
        return ResponseEntity.ok(rm);
    }

    @Transactional
    public ResponseEntity<ResponseModel> excluir(Integer id) {
        ResponseModel rm = new ResponseModel();

        EmprestimoModel emprestimo = emprestimoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Empréstimo não encontrado."));

        AlunoModel aluno = emprestimo.getAluno();
        if (aluno != null) {
            aluno.decrementarEmprestimos();
            alunoRepository.save(aluno);
        }

        emprestimoRepository.deleteById(id);
        rm.setMensagem("Empréstimo removido com sucesso.");
        return ResponseEntity.ok(rm);
    }

    // ================ MÉTODOS AUXILIARES (SEM ALTERAÇÕES) ================

    private static Penalidade calcularPenalidade(long diasAtraso) {
        if (diasAtraso <= 1)
            return Penalidade.REGISTRO;
        if (diasAtraso <= 5)
            return Penalidade.ADVERTENCIA;
        if (diasAtraso <= 7)
            return Penalidade.SUSPENSAO;
        if (diasAtraso <= 10)
            return Penalidade.BLOQUEIO;
        if (diasAtraso <= 90)
            return Penalidade.BLOQUEIO;
        return Penalidade.BANIMENTO;
    }

    private static boolean penalidadeMaisGrave(Penalidade nova, Penalidade atual) {
        if (nova == null)
            return false;
        if (atual == null)
            return true;
        return gravidade(nova) > gravidade(atual);
    }

    private static int gravidade(Penalidade p) {
        return switch (p) {
            case REGISTRO -> 1;
            case ADVERTENCIA -> 2;
            case SUSPENSAO -> 3;
            case BLOQUEIO -> 4;
            case BANIMENTO -> 5;
        };
    }

    public List<AlunoRankingDTO> gerarRankingAlunos(int top) {
        List<AlunoModel> alunos = alunoRepository.findAllByOrderByEmprestimosCountDesc();
        return alunos.stream()
                .limit(top)
                .map(a -> new AlunoRankingDTO(a.getMatricula(), a.getNomeCompleto(), a.getEmprestimosCount()))
                .toList();
    }

    public List<EmprestimoModel> buscarTodos() {
        return emprestimoRepository.findAll();
    }
}