package br.com.lumilivre.api.dto.student;

public record StudentRankingResponse(
        String registrationNumber,
        String fullName,
        long loanCount) {}
