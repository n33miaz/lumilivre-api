package br.com.lumilivre.api.dto.loan;

import java.time.OffsetDateTime;
import java.util.UUID;

import br.com.lumilivre.api.enums.LoanStatus;

public record LoanListItem(
        UUID id,
        LoanStatus status,
        String bookTitle,
        String copyCode,
        String studentName,
        String studentRegistrationNumber,
        String courseName,
        OffsetDateTime borrowedAt,
        OffsetDateTime dueAt) {
}
