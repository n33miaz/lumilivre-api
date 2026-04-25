package br.com.lumilivre.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.lumilivre.api.enums.LoanStatus;
import br.com.lumilivre.api.model.Loan;
import br.com.lumilivre.api.repository.LoanRepository;

@ExtendWith(MockitoExtension.class)
class OverdueMarkerJobTest {

    @Mock
    private LoanRepository loanRepository;

    @InjectMocks
    private OverdueMarkerJob job;

    @Captor
    private ArgumentCaptor<List<Loan>> loansCaptor;

    @Test
    void marcarAtrasadosDeveAtualizarEmprestimosAtivosVencidos() {
        Loan overdue = loan(LoanStatus.ACTIVE, OffsetDateTime.now().minusDays(1));
        when(loanRepository.findByStatusAndDueAtBefore(
                eq(LoanStatus.ACTIVE),
                org.mockito.ArgumentMatchers.any(OffsetDateTime.class)))
                .thenReturn(List.of(overdue));

        job.marcarAtrasados();

        verify(loanRepository).saveAll(loansCaptor.capture());

        assertThat(loansCaptor.getValue()).containsExactly(overdue);
        assertThat(overdue.getStatus()).isEqualTo(LoanStatus.OVERDUE);
    }

    @Test
    void marcarAtrasadosNaoDeveSalvarQuandoNaoHaEmprestimosVencidos() {
        when(loanRepository.findByStatusAndDueAtBefore(
                eq(LoanStatus.ACTIVE),
                org.mockito.ArgumentMatchers.any(OffsetDateTime.class)))
                .thenReturn(List.of());

        job.marcarAtrasados();

        verify(loanRepository, never()).saveAll(org.mockito.ArgumentMatchers.anyList());
    }

    private static Loan loan(LoanStatus status, OffsetDateTime dueAt) {
        Loan loan = new Loan();
        loan.setStatus(status);
        loan.setBorrowedAt(dueAt.minusDays(14));
        loan.setDueAt(dueAt);
        return loan;
    }
}
