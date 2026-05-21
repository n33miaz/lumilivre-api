package br.com.lumilivre.api.dto.dashboard;

public record DashboardStatsResponse(
        long activeLoans,
        long overdueLoans,
        long completedLoans,
        double avgReturnDays,
        long pendingRequests,
        long waitingReservations
) {}
