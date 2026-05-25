package br.com.lumilivre.api.service.infra.storage;

/**
 * Falhas de upload/recuperação no {@link StorageProvider}. Engloba erros do
 * provider externo (5xx, timeout) e validações locais (mime-type inválido,
 * arquivo vazio). Capturada nos services para produzir mensagens i18n via
 * {@code MessageResolver}; raramente deve ser propagada como 500.
 */
public class StorageException extends RuntimeException {

    public StorageException(String message) {
        super(message);
    }

    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
