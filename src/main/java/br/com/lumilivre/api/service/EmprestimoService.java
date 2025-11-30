package br.com.lumilivre.api.service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import br.com.lumilivre.api.dto.aluno.AlunoRankingResponse;
import br.com.lumilivre.api.dto.emprestimo.EmprestimoAtivoResponse;
import br.com.lumilivre.api.dto.emprestimo.EmprestimoDashboardResponse;
import br.com.lumilivre.api.dto.emprestimo.EmprestimoListagemResponse;
import br.com.lumilivre.api.dto.emprestimo.EmprestimoRequest;
import br.com.lumilivre.api.dto.emprestimo.EmprestimoResponse;
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
import br.com.lumilivre.api.service.infra.EmailService;

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
    public EmprestimoResponse cadastrar(EmprestimoRequest dto) {
        if (dto.getData_emprestimo() == null || dto.getData_devolucao() == null) {
            throw new RegraDeNegocioException("Datas de empréstimo e devolução são obrigatórias.");
        }
        if (dto.getData_devolucao().isBefore(dto.getData_emprestimo())) {
            throw new RegraDeNegocioException("A data de devolução não pode ser anterior à data de empréstimo.");
        }

        AlunoModel aluno = alunoRepository.findByMatricula(dto.getAluno_matricula())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Aluno não encontrado."));

        if (aluno.getPenalidade() != null) {
            LocalDateTime agora = LocalDateTime.now();

            if (aluno.getPenalidadeExpiraEm() != null && aluno.getPenalidadeExpiraEm().isBefore(agora)) {
                aluno.setPenalidade(null);
                aluno.setPenalidadeExpiraEm(null);
                alunoRepository.save(aluno);
            } else {
                throw new RegraDeNegocioException(
                        "O aluno possui penalidade ativa até " +
                                (aluno.getPenalidadeExpiraEm() != null ? aluno.getPenalidadeExpiraEm()
                                        : "indeterminado"));
            }
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

        return new EmprestimoResponse(salvo);
    }

    @Transactional
    public EmprestimoResponse atualizar(EmprestimoRequest dto) {
        EmprestimoModel emprestimo = emprestimoRepository.findById(dto.getId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Empréstimo não encontrado."));

        if (emprestimo.getStatusEmprestimo() == StatusEmprestimo.CONCLUIDO) {
            throw new RegraDeNegocioException("Este empréstimo já foi concluído e não pode ser alterado.");
        }

        emprestimo.setDataEmprestimo(dto.getData_emprestimo());
        emprestimo.setDataDevolucao(dto.getData_devolucao());

        EmprestimoModel salvo = emprestimoRepository.save(emprestimo);
        return new EmprestimoResponse(salvo);
    }

    @Transactional
    public EmprestimoResponse concluirEmprestimo(Integer id) {
        EmprestimoModel emprestimo = emprestimoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Empréstimo não encontrado."));

        if (emprestimo.getStatusEmprestimo() == StatusEmprestimo.CONCLUIDO) {
            throw new RegraDeNegocioException("Este empréstimo já foi concluído.");
        }

        LocalDateTime agora = LocalDateTime.now();
        AlunoModel aluno = emprestimo.getAluno();

        // calculo de penalidade
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

        return new EmprestimoResponse(salvo);
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

    public Page<EmprestimoListagemResponse> buscarEmprestimoParaListaAdmin(Pageable pageable) {
        Pageable pageableTratado = tratarOrdenacao(pageable);
        return emprestimoRepository.findEmprestimoParaListaAdmin(pageableTratado);
    }

    public Page<EmprestimoListagemResponse> buscarPorTexto(String texto, Pageable pageable) {
        Pageable pageableTratado = tratarOrdenacao(pageable);

        if (texto == null || texto.isBlank()) {
            return buscarAvancado(null, null, null, null, null, null, null, pageableTratado);
        }

        return emprestimoRepository.buscarPorTexto(texto, pageableTratado);
    }

    public List<EmprestimoResponse> listarEmprestimosAluno(String matricula) {
        return emprestimoRepository.findEmprestimosAtivos(matricula);
    }

    public List<EmprestimoResponse> listarHistorico(String matricula) {
        return emprestimoRepository.findHistoricoEmprestimos(matricula);
    }

    public List<EmprestimoDashboardResponse> listarEmprestimosAtivosEAtrasados() {
        return emprestimoRepository.findEmprestimosAtivosEAtrasados();
    }

    public List<EmprestimoAtivoResponse> buscarAtivosEAtrasados() {
        return emprestimoRepository.findAtivosEAtrasadosDTO();
    }

    public long getContagemEmprestimosAtivosEAtrasados() {
        return emprestimoRepository.countByStatusEmprestimoIn(
                List.of(StatusEmprestimo.ATIVO, StatusEmprestimo.ATRASADO));
    }

    public List<EmprestimoAtivoResponse> buscarApenasAtrasados() {
        return emprestimoRepository.findApenasAtrasadosDTO(LocalDate.now().atStartOfDay());
    }

    public Page<EmprestimoListagemResponse> buscarAvancado(
            StatusEmprestimo statusEmprestimo,
            String tombo,
            String livroNome,
            String alunoNomeCompleto,
            String dataEmprestimo,
            String dataDevolucao,
            LocalDateTime dataDevolucaoInicio,
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

        Pageable pageableFinal = tratarOrdenacao(pageable);

        String statusString = (statusEmprestimo != null) ? statusEmprestimo.name() : null;

        return emprestimoRepository.buscarAvancado(
                statusString,
                tomboFiltro,
                livroNomeFiltro,
                alunoNomeFiltro,
                dataEmprestimoInicio,
                null,
                dataDevolucaoInicio,
                dataDevolucaoFim,
                LocalDate.now().atStartOfDay(),
                pageableFinal);
    }

    public List<AlunoRankingResponse> gerarRankingAlunos(int top, Integer cursoId, Integer moduloId, Integer turnoId) {
        return alunoRepository.findRankingComFiltros(cursoId, moduloId, turnoId, PageRequest.of(0, top))
                .getContent();
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

    private Pageable tratarOrdenacao(Pageable pageable) {
        Sort.Order statusOrder = pageable.getSort().getOrderFor("status");

        if (statusOrder != null) {
            boolean isAsc = statusOrder.isAscending();

            if (isAsc) {
                return PageRequest.of(
                        pageable.getPageNumber(),
                        pageable.getPageSize(),
                        Sort.by(Sort.Direction.ASC, "ordemStatus", "dataDevolucao"));
            } else {
                return PageRequest.of(
                        pageable.getPageNumber(),
                        pageable.getPageSize(),
                        Sort.by(Sort.Order.asc("ordemStatus"), Sort.Order.desc("dataDevolucao")));
            }
        }

        return pageable;
    }
}