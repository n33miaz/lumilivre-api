package br.com.lumilivre.api.dto.dashboard;

public record TopLivroResponse(
        Long livroId,
        String titulo,
        String autor,
        String imagem,
        long totalEmprestimos,
        double avaliacao
) {}
