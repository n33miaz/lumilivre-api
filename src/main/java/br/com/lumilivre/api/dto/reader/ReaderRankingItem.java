package br.com.lumilivre.api.dto.reader;

public record ReaderRankingItem(
        String registrationNumber,
        String fullName,
        long loanCount) {
}
