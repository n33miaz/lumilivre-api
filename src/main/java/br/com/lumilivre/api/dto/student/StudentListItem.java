package br.com.lumilivre.api.dto.student;

import java.time.LocalDate;

import br.com.lumilivre.api.enums.PenaltyCode;

public record StudentListItem(
        PenaltyCode penaltyCode,
        String registrationNumber,
        String courseName,
        String fullName,
        LocalDate birthDate,
        String email,
        String phoneNumber) {
}
