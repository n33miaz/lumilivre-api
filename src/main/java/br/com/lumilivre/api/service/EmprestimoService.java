package br.com.lumilivre.api.service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.lumilivre.api.dto.responses.EmprestimoResponseDTO;
import br.com.lumilivre.api.dto.ListaEmprestimoDTO;
import br.com.lumilivre.api.dto.aluno.AlunoRankingResponse;
import br.com.lumilivre.api.dto.emprestimo.EmprestimoRequest;
import br.com.lumilivre.api.dto.emprestimo.ListaEmprestimoAtivoDTO;
import br.com.lumilivre.api.dto.emprestimo.EmprestimoDashboardResponse;
import br.com.lumilivre.api.enums.Penalidade;
import br.com.lumilivre.api.enums.StatusEmprestimo;
import br.com.lumilivre.api.enums.StatusLivro;
import br.com.lumilivre.api.exception.custom.RecursoNaoEncontradoException;
import br.com.lumilivre.api.exception.custom.RegraDeNegocioException;
import br.com.lumilivre.api.model.AlunoModel;
import br.com.lumilivre.api.model.EmprestimoModel;
import br.com.lumilivre.api.model.ExemplarModel;
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

    // ================ MÉTODOS DE ESCRITA ================

    @Transactional
    public EmprestimoResponseDTO cadastrar(EmprestimoRequest dto) {
        if (dto.getData_emprestimo() == null || dto.getData_devolucao() == null) {
            throw new RegraDeNegocioException("Datas de empréstimo e devolução são obrigatórias.");
        }
        if (dto.getData_devolucao().isBefore(dto.getData_emprestimo())) {
            throw new RegraDeNegocioException("A data de devolução não pode ser anterior à data de empréstimo.");
        }

        AlunoModel aluno = alunoRepository.findByMatricula(dto.getAluno_matricula())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Aluno não encontrado."));

        if (aluno.getPenalidade() != null) {
            if (aluno.getPenalidadeExpiraEm() != null && aluno.getPenalidadeExpiraEm().isAfter(LocalDateTime.now())) {
                throw new RegraDeNegocioException(
                        "O aluno possui penalidade ativa até " + aluno.getPenalidadeExpiraEm());
            }
            // TODO: Se já expirou, limpar a penalidade (Lazycleanup)
        }

        long emprestimosAtivosAluno = emprestimoRepository
                .countByAlunoMatriculaAndStatusEmprestimo(aluno.getMatricula(), StatusEmprestimo.ATIVO);

        if (emprestimosAtivosAluno >= LIMITE_EMPRESTIMOS_ATIVOS) {
            throw new RegraDeNegocioException(
                    "O aluno já possui o limite de " + LIMITE_EMPRESTIMOS_ATIVOS + " empréstimos ativos.");
        }

        ExemplarModel exemplar = exemplarRepository.findByTombo(dto.getExemplar_tombo())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Exemplar não encontrado."));

        if (exemplar.getStatus_livro() != StatusLivro.DISPONIVEL) {
            throw new RegraDeNegocioException("O exemplar não está disponível para empréstimo.");
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

        EmprestimoModel salvo = emprestimoRepository.save(emprestimo);

        enviarEmailEmprestimo(aluno, exemplar, dto);

        return new EmprestimoResponseDTO(salvo);
    }

    @Transactional
    public EmprestimoResponseDTO atualizar(EmprestimoRequest dto) {
        EmprestimoModel emprestimo = emprestimoRepository.findById(dto.getId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Empréstimo não encontrado."));

        if (emprestimo.getStatusEmprestimo() == StatusEmprestimo.CONCLUIDO) {
            throw new RegraDeNegocioException("Este empréstimo já foi concluído e não pode ser alterado.");
        }

        emprestimo.setDataEmprestimo(dto.getData_emprestimo());
        emprestimo.setDataDevolucao(dto.getData_devolucao());

        EmprestimoModel salvo = emprestimoRepository.save(emprestimo);
        return new EmprestimoResponseDTO(salvo);
    }

    @Transactional
    public EmprestimoResponseDTO concluirEmprestimo(Integer id) {
        EmprestimoModel emprestimo = emprestimoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Empréstimo não encontrado."));

        if (emprestimo.getStatusEmprestimo() == StatusEmprestimo.CONCLUIDO) {
            throw new RegraDeNegocioException("Este empréstimo já foi concluído.");
        }

        LocalDateTime agora = LocalDateTime.now();
        AlunoModel aluno = emprestimo.getAluno();

        // Cálculo de Penalidade
        if (emprestimo.getDataDevolucao().isBefore(agora)) {
            long diasDeAtraso = Duration.between(emprestimo.getDataDevolucao(), agora).toDays();
            Penalidade novaPenalidade = Penalidade.fromDiasDeAtraso(diasDeAtraso);
            emprestimo.setPenalidade(novaPenalidade);

            if (novaPenalidade.isMaisGraveQue(aluno.getPenalidade())) {
                aluno.setPenalidade(novaPenalidade);
                aluno.setPenalidadeExpiraEm(agora.plusDays(7));
                alunoRepository.save(aluno);
            }
        }

        emprestimo.setStatusEmprestimo(StatusEmprestimo.CONCLUIDO);

        ExemplarModel exemplar = emprestimo.getExemplar();
        exemplar.setStatus_livro(StatusLivro.DISPONIVEL);
        exemplarRepository.save(exemplar);

        EmprestimoModel salvo = emprestimoRepository.save(emprestimo);

        enviarEmailConclusao(aluno, exemplar, emprestimo);

        return new EmprestimoResponseDTO(salvo);
    }

    @Transactional
    public void excluir(Integer id) {
        EmprestimoModel emprestimo = emprestimoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Empréstimo não encontrado."));

        AlunoModel aluno = emprestimo.getAluno();
        if (aluno != null) {
            aluno.decrementarEmprestimos();
            alunoRepository.save(aluno);
        }

        if (emprestimo.getStatusEmprestimo() == StatusEmprestimo.ATIVO ||
                emprestimo.getStatusEmprestimo() == StatusEmprestimo.ATRASADO) {
            ExemplarModel exemplar = emprestimo.getExemplar();
            if (exemplar != null) {
                exemplar.setStatus_livro(StatusLivro.DISPONIVEL);
                exemplarRepository.save(exemplar);
            }
        }

        emprestimoRepository.deleteById(id);
    }

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

    // Atenção: Aqui usamos o DTO antigo
    public List<br.com.lumilivre.api.dto.emprestimo.EmprestimoResponse> listarEmprestimosAluno(String matricula) {
        return emprestimoRepository.findEmprestimosAtivos(matricula);
    }

    public List<br.com.lumilivre.api.dto.emprestimo.EmprestimoResponse> listarHistorico(String matricula) {
        return emprestimoRepository.findHistoricoEmprestimos(matricula);
    }

    public List<EmprestimoDashboardResponse> listarEmprestimosAtivosEAtrasados() {
        return emprestimoRepository.findEmprestimosAtivosEAtrasados();
    }

    public List<ListaEmprestimoAtivoDTO> buscarAtivosEAtrasados() {
        return emprestimoRepository.findAtivosEAtrasadosDTO();
    }

    public long getContagemEmprestimosAtivosEAtrasados() {
        return emprestimoRepository.countByStatusEmprestimoIn(
                List.of(StatusEmprestimo.ATIVO, StatusEmprestimo.ATRASADO));
    }

    public List<ListaEmprestimoAtivoDTO> buscarApenasAtrasados() {
        return emprestimoRepository.findApenasAtrasadosDTO();
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

    public List<AlunoRankingResponse> gerarRankingAlunos(int top) {
        List<AlunoModel> alunos = alunoRepository.findAllByOrderByEmprestimosCountDesc();
        return alunos.stream()
                .limit(top)
                .map(a -> new AlunoRankingResponse(a.getMatricula(), a.getNomeCompleto(), a.getEmprestimosCount()))
                .toList();
    }

    public List<EmprestimoModel> buscarTodos() {
        return emprestimoRepository.findAll();
    }

    // ================ MÉTODOS AUXILIARES ================

    private void enviarEmailEmprestimo(AlunoModel aluno, ExemplarModel exemplar, EmprestimoRequest dto) {
        try {
            String mensagemEmail = String.format(
                    "Olá %s,\n\nSeu empréstimo do livro '%s' foi registrado com sucesso.\n" +
                            "Data de empréstimo: %s\nData de devolução: %s\n\nAtenciosamente,\nBiblioteca LumiLivre",
                    aluno.getNomeCompleto(),
                    exemplar.getLivro().getNome(),
                    dto.getData_emprestimo(),
                    dto.getData_devolucao());
            emailService.enviarEmail(aluno.getEmail(), "Empréstimo registrado", mensagemEmail);
        } catch (Exception e) {
            System.err.println("Erro ao enviar e-mail de empréstimo: " + e.getMessage());
        }
    }

    private void enviarEmailConclusao(AlunoModel aluno, ExemplarModel exemplar, EmprestimoModel emprestimo) {
        try {
            String mensagemEmail = String.format(
                    "Olá %s,\n\nSeu empréstimo do livro '%s' foi concluído.\n" +
                            "Status da penalidade: %s\n\nAtenciosamente,\nBiblioteca LumiLivre",
                    aluno.getNomeCompleto(),
                    exemplar.getLivro().getNome(),
                    emprestimo.getPenalidade() != null ? emprestimo.getPenalidade().getStatus() : "Nenhuma");
            emailService.enviarEmail(aluno.getEmail(), "Empréstimo concluído", mensagemEmail);
        } catch (Exception e) {
            System.err.println("Erro ao enviar e-mail de conclusão: " + e.getMessage());
        }
    }
}