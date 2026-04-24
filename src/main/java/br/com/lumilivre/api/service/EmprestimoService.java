package br.com.lumilivre.api.service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.JpaSort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import br.com.lumilivre.api.domain.policy.BookAvailabilityPolicy;
import br.com.lumilivre.api.domain.policy.LoanPolicy;
import br.com.lumilivre.api.domain.policy.PenaltyPolicy;
import br.com.lumilivre.api.enums.StatusReserva;
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
import br.com.lumilivre.api.model.Student;
import br.com.lumilivre.api.model.EmprestimoModel;
import br.com.lumilivre.api.model.ExemplarModel;
import br.com.lumilivre.api.model.OutboxEvent.EventType;
import br.com.lumilivre.api.repository.StudentRepository;
import br.com.lumilivre.api.repository.EmprestimoRepository;
import br.com.lumilivre.api.repository.ExemplarRepository;
import br.com.lumilivre.api.repository.ReservaRepository;
import br.com.lumilivre.api.security.Auditable;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmprestimoService {

    private final StudentRepository alunoRepository;
    private final ExemplarRepository exemplarRepository;
    private final EmprestimoRepository emprestimoRepository;
    private final ReservaRepository reservaRepository;
    private final OutboxPublisherService outboxPublisher;

    // ================ MÉTODOS DE ESCRITA ================

    @Auditable(action = "LOAN_CREATED", targetParam = "#dto.aluno_matricula")
    @Transactional
    @CacheEvict(value = {
            "dashboard_stats_emprestimos",
            "dashboard_atrasados_count",
            "dashboard_atrasados_list"
    }, allEntries = true)
    public EmprestimoResponse cadastrar(EmprestimoRequest dto) {
        if (dto.getData_emprestimo() == null || dto.getData_devolucao() == null) {
            throw new RegraDeNegocioException("Datas de empréstimo e devolução são obrigatórias.");
        }
        if (dto.getData_devolucao().isBefore(dto.getData_emprestimo())) {
            throw new RegraDeNegocioException("A data de devolução não pode ser anterior à data de empréstimo.");
        }

        Student aluno = alunoRepository.findByMatricula(dto.getAluno_matricula())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Aluno não encontrado."));

        // Limpa penalidade expirada antes de validar
        LocalDateTime agora = LocalDateTime.now();
        if (aluno.getPenalidadeExpiraEm() != null && aluno.getPenalidadeExpiraEm().isBefore(agora)) {
            aluno.setPenalidade(null);
            aluno.setPenalidadeExpiraEm(null);
            alunoRepository.save(aluno);
        }

        long emprestimosAtivos = emprestimoRepository
                .countByAlunoMatriculaAndStatusEmprestimo(aluno.getMatricula(), StatusEmprestimo.ACTIVE);
        LoanPolicy.validateNewLoan(emprestimosAtivos, aluno.getPenalidadeExpiraEm());

        ExemplarModel exemplar = exemplarRepository.findByTombo(dto.getExemplar_tombo())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Exemplar não encontrado."));

        BookAvailabilityPolicy.validateAvailable(exemplar.getStatus_livro());

        EmprestimoModel emprestimo = new EmprestimoModel();
        emprestimo.setAluno(aluno);
        emprestimo.setExemplar(exemplar);
        emprestimo.setDataEmprestimo(dto.getData_emprestimo());
        emprestimo.setDataDevolucao(dto.getData_devolucao());
        emprestimo.setStatusEmprestimo(StatusEmprestimo.ACTIVE);

        exemplar.setStatus_livro(StatusLivro.BORROWED);
        exemplarRepository.save(exemplar);

        aluno.incrementarEmprestimos();
        alunoRepository.save(aluno);

        EmprestimoModel salvo = emprestimoRepository.save(emprestimo);

        enviarEmailEmprestimo(aluno, exemplar, dto);

        return new EmprestimoResponse(salvo);
    }

    @Transactional
    @CacheEvict(value = {
            "dashboard_stats_emprestimos",
            "dashboard_atrasados_count",
            "dashboard_atrasados_list"
    }, allEntries = true)
    public EmprestimoResponse atualizar(EmprestimoRequest dto) {
        EmprestimoModel emprestimo = emprestimoRepository.findById(dto.getId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Empréstimo não encontrado."));

        if (emprestimo.getStatusEmprestimo() == StatusEmprestimo.COMPLETED) {
            throw new RegraDeNegocioException("Este empréstimo já foi concluído e não pode ser alterado.");
        }

        emprestimo.setDataEmprestimo(dto.getData_emprestimo());
        emprestimo.setDataDevolucao(dto.getData_devolucao());

        EmprestimoModel salvo = emprestimoRepository.save(emprestimo);
        return new EmprestimoResponse(salvo);
    }

    @Auditable(action = "LOAN_RETURNED", targetParam = "#id")
    @Transactional
    @CacheEvict(value = {
            "dashboard_stats_emprestimos",
            "dashboard_atrasados_count",
            "dashboard_atrasados_list"
    }, allEntries = true)
    public EmprestimoResponse concluirEmprestimo(Integer id) {
        EmprestimoModel emprestimo = emprestimoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Empréstimo não encontrado."));

        if (emprestimo.getStatusEmprestimo() == StatusEmprestimo.COMPLETED) {
            throw new RegraDeNegocioException("Este empréstimo já foi concluído.");
        }

        LocalDateTime agora = LocalDateTime.now();
        Student aluno = emprestimo.getAluno();

        // cálculo de penalidade via PenaltyPolicy
        if (emprestimo.getDataDevolucao().isBefore(agora)) {
            long diasDeAtraso = Duration.between(emprestimo.getDataDevolucao(), agora).toDays();
            Penalidade novaPenalidade = PenaltyPolicy.calculate(diasDeAtraso);
            emprestimo.setPenalidade(novaPenalidade);

            if (PenaltyPolicy.isMoreSevere(novaPenalidade, aluno.getPenalidade())) {
                aluno.setPenalidade(novaPenalidade);
                aluno.setPenalidadeExpiraEm(agora.plusDays(7));
                alunoRepository.save(aluno);
            }
        }

        emprestimo.setStatusEmprestimo(StatusEmprestimo.COMPLETED);

        ExemplarModel exemplar = emprestimo.getExemplar();
        exemplar.setStatus_livro(StatusLivro.AVAILABLE);
        exemplarRepository.save(exemplar);

        EmprestimoModel salvo = emprestimoRepository.save(emprestimo);

        enviarEmailConclusao(aluno, exemplar, emprestimo);

        // Notifica o próximo da fila de reservas para este livro
        reservaRepository.findFirstByLivroIdAndStatusOrderByPosicaoFilaAsc(
                exemplar.getLivro().getId(), StatusReserva.WAITING)
                .ifPresent(proxima -> {
                    proxima.setStatus(StatusReserva.READY);
                    LocalDateTime agora2 = LocalDateTime.now();
                    proxima.setNotificadoEm(agora2);
                    proxima.setExpiraEm(agora2.plusDays(2));
                    reservaRepository.save(proxima);
                    outboxPublisher.publish(EventType.REQUEST_ACCEPTED,
                            proxima.getAluno().getEmail(),
                            "Livro disponível para retirada",
                            "O livro '" + exemplar.getLivro().getNome() +
                            "' está disponível. Retire até " + proxima.getExpiraEm().toLocalDate() + ".");
                });

        return new EmprestimoResponse(salvo);
    }

    @Transactional
    public void excluir(Integer id) {
        EmprestimoModel emprestimo = emprestimoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Empréstimo não encontrado."));

        Student aluno = emprestimo.getAluno();
        if (aluno != null) {
            aluno.decrementarEmprestimos();
            alunoRepository.save(aluno);
        }

        if (emprestimo.getStatusEmprestimo() == StatusEmprestimo.ACTIVE ||
                emprestimo.getStatusEmprestimo() == StatusEmprestimo.OVERDUE) {
            ExemplarModel exemplar = emprestimo.getExemplar();
            if (exemplar != null) {
                exemplar.setStatus_livro(StatusLivro.AVAILABLE);
                exemplarRepository.save(exemplar);
            }
        }

        emprestimoRepository.deleteById(id);
    }

    @Auditable(action = "LOAN_RENEWED", targetParam = "#id")
    @Transactional
    @CacheEvict(value = {
            "dashboard_stats_emprestimos",
            "dashboard_atrasados_count",
            "dashboard_atrasados_list"
    }, allEntries = true)
    public EmprestimoResponse renovar(Integer id) {
        EmprestimoModel emprestimo = emprestimoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Empréstimo não encontrado."));

        if (emprestimo.getStatusEmprestimo() == StatusEmprestimo.COMPLETED) {
            throw new RegraDeNegocioException("Empréstimo já concluído não pode ser renovado.");
        }

        Student aluno = emprestimo.getAluno();
        Long livroId = emprestimo.getExemplar().getLivro().getId();

        boolean hasReservation = reservaRepository
                .findFirstByLivroIdAndStatusOrderByPosicaoFilaAsc(livroId, StatusReserva.WAITING)
                .map(r -> !r.getAluno().getMatricula().equals(aluno.getMatricula()))
                .orElse(false);

        LoanPolicy.validateRenewal(
                emprestimo.getRenovacoes(),
                hasReservation,
                aluno.getPenalidadeExpiraEm());

        emprestimo.setDataDevolucao(emprestimo.getDataDevolucao().plusDays(LoanPolicy.RENEWAL_DAYS));
        emprestimo.setRenovacoes(emprestimo.getRenovacoes() + 1);
        emprestimo.setStatusEmprestimo(StatusEmprestimo.ACTIVE);

        EmprestimoModel salvo = emprestimoRepository.save(emprestimo);

        outboxPublisher.publish(EventType.REQUEST_ACCEPTED, aluno.getEmail(),
                "Empréstimo renovado",
                "Seu empréstimo do livro '" + emprestimo.getExemplar().getLivro().getNome() +
                "' foi renovado. Nova data de devolução: " + salvo.getDataDevolucao().toLocalDate() + ".");

        return new EmprestimoResponse(salvo);
    }

    // ================ MÉTODOS DE BUSCA ================

    public Page<EmprestimoListagemResponse> buscarEmprestimoParaListaAdmin(Pageable pageable) {
        Pageable pageableTratado = tratarOrdenacao(pageable);
        return emprestimoRepository.findEmprestimoParaListaAdmin(pageableTratado);
    }

    public Page<EmprestimoListagemResponse> buscarPorTexto(String texto, Pageable pageable) {
        Pageable pageableNativo = pageable;

        Sort.Order statusOrder = pageable.getSort().getOrderFor("status");

        if (statusOrder != null) {
            boolean isAsc = statusOrder.isAscending();
            if (isAsc) {
                pageableNativo = PageRequest.of(
                        pageable.getPageNumber(),
                        pageable.getPageSize(),
                        JpaSort.unsafe(Sort.Direction.ASC,
                                "(CASE WHEN e.statusEmprestimo = 'COMPLETED' THEN 1 ELSE 0 END)",
                                "dataDevolucao"));
            } else {
                pageableNativo = PageRequest.of(
                        pageable.getPageNumber(),
                        pageable.getPageSize(),
                        JpaSort.unsafe(Sort.Direction.ASC,
                                "(CASE WHEN e.statusEmprestimo = 'COMPLETED' THEN 1 ELSE 0 END)")
                                .and(JpaSort.unsafe(Sort.Direction.DESC, "dataDevolucao")));
            }
        }

        if (texto == null || texto.isBlank()) {
            return buscarAvancado(null, null, null, null, null, null, null, tratarOrdenacao(pageable));
        }

        return emprestimoRepository.buscarPorTexto(texto, pageableNativo);
    }

    public List<EmprestimoResponse> listarEmprestimosAluno(String matricula) {
        return emprestimoRepository.findEmprestimosAtivos(matricula);
    }

    public List<EmprestimoResponse> listarHistorico(String matricula) {
        return emprestimoRepository.findHistoricoEmprestimos(matricula);
    }

    @Cacheable(value = "dashboard_atrasados_list")
    public List<EmprestimoDashboardResponse> listarEmprestimosAtivosEAtrasados() {
        return emprestimoRepository.findEmprestimosAtivosEAtrasados();
    }

    public List<EmprestimoAtivoResponse> buscarAtivosEAtrasados() {
        return emprestimoRepository.findAtivosEAtrasadosDTO();
    }

    @Cacheable(value = "dashboard_atrasados_count")
    public long getContagemAtrasadosReal() {
        LocalDateTime agora = LocalDateTime.now();
        return emprestimoRepository.countByStatusEmprestimoIn(List.of(StatusEmprestimo.OVERDUE))
                + emprestimoRepository.findByStatusEmprestimoAndDataDevolucaoBefore(StatusEmprestimo.ACTIVE, agora)
                        .size();
    }

    @Cacheable(value = "dashboard_stats_emprestimos")
    public long getContagemEmprestimosAtivosEAtrasados() {
        return emprestimoRepository.countByStatusEmprestimoIn(
                List.of(StatusEmprestimo.ACTIVE, StatusEmprestimo.OVERDUE));
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

    private void enviarEmailEmprestimo(Student aluno, ExemplarModel exemplar, EmprestimoRequest dto) {
        String body = String.format(
                "Olá %s,\n\nSeu empréstimo do livro '%s' foi registrado com sucesso.\n" +
                        "Data de empréstimo: %s\nData de devolução: %s\n\nAtenciosamente,\nBiblioteca LumiLivre",
                aluno.getNomeCompleto(),
                exemplar.getLivro().getNome(),
                dto.getData_emprestimo(),
                dto.getData_devolucao());
        outboxPublisher.publish(EventType.LOAN_CREATED, aluno.getEmail(), "Empréstimo registrado", body);
    }

    private void enviarEmailConclusao(Student aluno, ExemplarModel exemplar, EmprestimoModel emprestimo) {
        String body = String.format(
                "Olá %s,\n\nSeu empréstimo do livro '%s' foi concluído.\n" +
                        "Status da penalidade: %s\n\nAtenciosamente,\nBiblioteca LumiLivre",
                aluno.getNomeCompleto(),
                exemplar.getLivro().getNome(),
                emprestimo.getPenalidade() != null ? emprestimo.getPenalidade().getStatus() : "Nenhuma");
        outboxPublisher.publish(EventType.LOAN_RETURNED, aluno.getEmail(), "Empréstimo concluído", body);
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
