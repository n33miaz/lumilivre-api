package br.com.lumilivre.api.service.infra.storage;

import java.util.Locale;

/**
 * Buckets lógicos suportados pelo {@link StorageProvider}. Cada implementação
 * mapeia esse enum para o nome físico do bucket no provider (Supabase, S3, MinIO,
 * filesystem local, etc.). Mantém o domínio livre de strings mágicas.
 */
public enum StorageBucket {
    COVERS("covers"),
    THESES("theses"),
    AVATARS("avatars");

    private final String folder;

    StorageBucket(String folder) {
        this.folder = folder;
    }

    /** Nome do diretório/objeto-prefixo associado ao bucket. */
    public String folder() {
        return folder;
    }

    /**
     * Conversão tolerante a partir de strings históricas usadas pelo legado
     * ("capas"/"covers", "tccs"/"theses", "avatars"). Mantida temporariamente
     * para suportar a transição dos consumidores até remoção do alias PT-BR.
     */
    public static StorageBucket fromLegacy(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Bucket type is required.");
        }
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "capas", "covers" -> COVERS;
            case "tccs", "theses" -> THESES;
            case "avatars" -> AVATARS;
            default -> throw new IllegalArgumentException(
                    "Invalid bucket '" + value + "'. Expected COVERS/THESES/AVATARS.");
        };
    }
}
