package br.com.lumilivre.api.dto.student;

public record StudentRankingItem(
        String registrationNumber,
        String fullName,
        long loanCount) {
}
