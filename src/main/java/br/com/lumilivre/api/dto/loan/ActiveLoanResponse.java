package br.com.lumilivre.api.dto.loan;

import java.time.LocalDate;
import java.util.UUID;

import br.com.lumilivre.api.dto.common.LocalizedEnum;

public record ActiveLoanResponse(
        UUID id,
        String bookTitle,
        String studentName,
        String studentRegistrationNumber,
        String copyCode,
        LocalDate borrowedAt,
        LocalDate dueAt,
        LocalizedEnum status) {
}
