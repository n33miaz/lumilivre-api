package br.com.lumilivre.api.dto.dashboard;

import java.time.LocalDate;

public record EmprestimosPorMesResponse(
        LocalDate mes,
        long total
) {}
