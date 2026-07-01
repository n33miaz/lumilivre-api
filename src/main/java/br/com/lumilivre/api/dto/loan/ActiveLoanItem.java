package br.com.lumilivre.api.dto.loan;

import java.time.LocalDate;
import java.util.UUID;

import br.com.lumilivre.api.enums.LoanStatus;

public record ActiveLoanItem(
        UUID id,
        String bookTitle,
        String readerName,
        String readerRegistrationNumber,
        String copyCode,
        LocalDate borrowedAt,
        LocalDate dueAt,
        LoanStatus status) {
}
