package br.com.lumilivre.api.mapper.v2;

import java.util.List;

import br.com.lumilivre.api.dto.dashboard.DashboardStatsResponse;
import br.com.lumilivre.api.dto.dashboard.DashboardStatsV2Response;
import br.com.lumilivre.api.dto.dashboard.EmprestimosPorMesResponse;
import br.com.lumilivre.api.dto.dashboard.LoansByMonthResponse;
import br.com.lumilivre.api.dto.dashboard.TopBookResponse;
import br.com.lumilivre.api.dto.dashboard.TopLivroResponse;
import org.springframework.stereotype.Component;

@Component
public class DashboardMapper {

    public DashboardStatsV2Response toV2Stats(DashboardStatsResponse v1) {
        return new DashboardStatsV2Response(
                v1.emprestimosAtivos(),
                v1.emprestimosAtrasados(),
                v1.emprestimosConcluidos(),
                v1.mediaDiasDevolucao(),
                v1.solicitacoesPendentes(),
                v1.reservasAguardando()
        );
    }

    public TopBookResponse toTopBook(TopLivroResponse v1) {
        return new TopBookResponse(
                v1.livroId(),
                v1.titulo(),
                v1.autor(),
                v1.imagem(),
                v1.totalEmprestimos(),
                v1.avaliacao()
        );
    }

    public LoansByMonthResponse toLoansByMonth(EmprestimosPorMesResponse v1) {
        return new LoansByMonthResponse(v1.mes(), v1.total());
    }

    public List<TopBookResponse> toTopBooks(List<TopLivroResponse> v1List) {
        return v1List.stream().map(this::toTopBook).toList();
    }

    public List<LoansByMonthResponse> toLoansByMonthList(List<EmprestimosPorMesResponse> v1List) {
        return v1List.stream().map(this::toLoansByMonth).toList();
    }
}
