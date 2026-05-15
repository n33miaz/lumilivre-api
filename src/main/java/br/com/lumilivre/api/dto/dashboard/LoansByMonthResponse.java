package br.com.lumilivre.api.dto.dashboard;

import java.time.LocalDate;

public record LoansByMonthResponse(LocalDate month, long total) {}
