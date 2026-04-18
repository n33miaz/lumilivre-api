package br.com.lumilivre.api.dto.dashboard;

public record DashboardStatsResponse(
        long emprestimosAtivos,
        long emprestimosAtrasados,
        long emprestimosConcluidos,
        double mediaDiasDevolucao,
        long solicitacoesPendentes,
        long reservasAguardando
) {}
