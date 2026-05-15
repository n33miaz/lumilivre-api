package br.com.lumilivre.api.dto.dashboard;

import java.util.UUID;

public record TopBookResponse(
        UUID bookId,
        String title,
        String author,
        String coverUrl,
        long totalLoans,
        double rating
) {}
