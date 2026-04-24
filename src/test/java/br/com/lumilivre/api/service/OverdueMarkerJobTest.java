package br.com.lumilivre.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.lumilivre.api.enums.StatusEmprestimo;
import br.com.lumilivre.api.model.EmprestimoModel;
import br.com.lumilivre.api.repository.EmprestimoRepository;

@ExtendWith(MockitoExtension.class)
class OverdueMarkerJobTest {

    @Mock
    private EmprestimoRepository emprestimoRepository;

    @InjectMocks
    private OverdueMarkerJob job;

    @Captor
    private ArgumentCaptor<List<EmprestimoModel>> emprestimosCaptor;

    @Test
    void marcarAtrasadosDeveAtualizarEmprestimosAtivosVencidos() {
        EmprestimoModel vencido = emprestimo(StatusEmprestimo.ACTIVE, LocalDateTime.now().minusDays(1));
        when(emprestimoRepository.findByStatusEmprestimoAndDataDevolucaoBefore(
                eq(StatusEmprestimo.ACTIVE),
                org.mockito.ArgumentMatchers.any(LocalDateTime.class)))
                .thenReturn(List.of(vencido));

        job.marcarAtrasados();

        verify(emprestimoRepository).saveAll(emprestimosCaptor.capture());

        assertThat(emprestimosCaptor.getValue()).containsExactly(vencido);
        assertThat(vencido.getStatusEmprestimo()).isEqualTo(StatusEmprestimo.OVERDUE);
    }

    @Test
    void marcarAtrasadosNaoDeveSalvarQuandoNaoHaEmprestimosVencidos() {
        when(emprestimoRepository.findByStatusEmprestimoAndDataDevolucaoBefore(
                eq(StatusEmprestimo.ACTIVE),
                org.mockito.ArgumentMatchers.any(LocalDateTime.class)))
                .thenReturn(List.of());

        job.marcarAtrasados();

        verify(emprestimoRepository, never()).saveAll(org.mockito.ArgumentMatchers.anyList());
    }

    private static EmprestimoModel emprestimo(StatusEmprestimo status, LocalDateTime dataDevolucao) {
        EmprestimoModel emprestimo = new EmprestimoModel();
        emprestimo.setStatusEmprestimo(status);
        emprestimo.setDataEmprestimo(dataDevolucao.minusDays(14));
        emprestimo.setDataDevolucao(dataDevolucao);
        return emprestimo;
    }
}
