package br.com.lumilivre.api.dto.dashboard;

public record DashboardStatsV2Response(
        long activeLoans,
        long overdueLoans,
        long completedLoans,
        double avgReturnDays,
        long pendingRequests,
        long waitingReservations
) {}
