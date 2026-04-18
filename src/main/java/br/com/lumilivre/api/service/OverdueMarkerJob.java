package br.com.lumilivre.api.service;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.lumilivre.api.enums.StatusEmprestimo;
import br.com.lumilivre.api.model.EmprestimoModel;
import br.com.lumilivre.api.repository.EmprestimoRepository;
import lombok.RequiredArgsConstructor;

/**
 * Marca empréstimos ATIVO com dataDevolucao < agora como ATRASADO.
 * Executa diariamente às 03:00.
 */
@Service
@RequiredArgsConstructor
public class OverdueMarkerJob {

    private static final Logger log = LoggerFactory.getLogger(OverdueMarkerJob.class);

    private final EmprestimoRepository emprestimoRepository;

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void marcarAtrasados() {
        LocalDateTime agora = LocalDateTime.now();

        List<EmprestimoModel> vencidos = emprestimoRepository
                .findByStatusEmprestimoAndDataDevolucaoBefore(StatusEmprestimo.ATIVO, agora);

        if (vencidos.isEmpty()) {
            log.info("OverdueMarkerJob: nenhum empréstimo para marcar como atrasado.");
            return;
        }

        for (EmprestimoModel e : vencidos) {
            e.setStatusEmprestimo(StatusEmprestimo.ATRASADO);
        }

        emprestimoRepository.saveAll(vencidos);
        log.info("OverdueMarkerJob: {} empréstimo(s) marcado(s) como ATRASADO.", vencidos.size());
    }
}
