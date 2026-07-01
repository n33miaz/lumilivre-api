package br.com.lumilivre.api.dto.reader;

public record ReaderRankingResponse(
        String registrationNumber,
        String fullName,
        long loanCount) {}
