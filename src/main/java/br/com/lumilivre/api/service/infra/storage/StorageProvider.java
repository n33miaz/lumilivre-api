package br.com.lumilivre.api.service.infra.storage;

import org.springframework.web.multipart.MultipartFile;

/**
 * Strategy de armazenamento de blobs (capas de livros, PDFs de TCC, avatares).
 * Implementações disponíveis:
 *
 * <ul>
 *   <li>{@code SupabaseStorageProvider} — default, Supabase Storage via REST.</li>
 *   <li>{@code LocalFilesystemStorageProvider} — fallback em disco local.</li>
 * </ul>
 *
 * Seleção via property {@code lumilivre.storage.provider} (supabase | local).
 * ADR-011 registra a decisão.
 */
public interface StorageProvider {

    /**
     * Identifica a implementação (útil em logs/auditoria/health).
     * Ex.: {@code "supabase"}, {@code "local"}.
     */
    String name();

    /**
     * Faz upload do {@code file} para o {@code bucket} indicado e devolve a URL
     * pública/acessível pelo client. A geração da URL é responsabilidade do
     * provider — consumidores nunca devem montar URLs concatenando strings.
     *
     * @throws StorageException quando o upload falha ou o conteúdo é inválido.
     */
    String upload(MultipartFile file, StorageBucket bucket);
}
