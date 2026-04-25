package br.com.lumilivre.api.dto.dashboard;

import java.util.UUID;

public record TopLivroResponse(
        UUID livroId,
        String titulo,
        String autor,
        String imagem,
        long totalEmprestimos,
        double avaliacao
) {}
