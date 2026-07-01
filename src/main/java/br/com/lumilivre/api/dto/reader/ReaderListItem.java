package br.com.lumilivre.api.dto.reader;

import java.time.LocalDate;

import br.com.lumilivre.api.enums.PenaltyCode;

public record ReaderListItem(
        PenaltyCode penaltyCode,
        String registrationNumber,
        String courseName,
        String readerCategory,
        String fullName,
        LocalDate birthDate,
        String email,
        String phoneNumber) {
}
