package br.com.lumilivre.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;

import br.com.lumilivre.api.enums.LoanRequestStatus;
import br.com.lumilivre.api.enums.LoanStatus;
import br.com.lumilivre.api.repository.LoanRepository;
import br.com.lumilivre.api.repository.LoanRequestRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BusinessMetricsServiceTest {

    @Mock
    private LoanRepository loanRepository;

    @Mock
    private LoanRequestRepository loanRequestRepository;

    @Test
    void registerGaugesPublishesCurrentRepositoryValues() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        when(loanRepository.countByStatusIn(List.of(LoanStatus.ACTIVE))).thenReturn(4L);
        when(loanRepository.countByStatusIn(List.of(LoanStatus.OVERDUE))).thenReturn(1L);
        when(loanRequestRepository.countByStatus(LoanRequestStatus.PENDING)).thenReturn(2L);
        when(loanRepository.avgReturnDays()).thenReturn(6.5);

        new BusinessMetricsService(registry, loanRepository, loanRequestRepository).registerGauges();

        assertThat(registry.get("loans.active").gauge().value()).isEqualTo(4.0);
        assertThat(registry.get("loans.overdue").gauge().value()).isEqualTo(1.0);
        assertThat(registry.get("requests.pending").gauge().value()).isEqualTo(2.0);
        assertThat(registry.get("returns.avg_days").gauge().value()).isEqualTo(6.5);
    }

    @Test
    void averageReturnGaugeFallsBackToZeroWhenRepositoryReturnsNull() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        when(loanRepository.avgReturnDays()).thenReturn(null);

        new BusinessMetricsService(registry, loanRepository, loanRequestRepository).registerGauges();

        assertThat(registry.get("returns.avg_days").gauge().value()).isZero();
    }
}
