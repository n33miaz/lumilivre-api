package br.com.lumilivre.api.enums;

/**
 * Discriminador de {@link br.com.lumilivre.api.model.AppContent}. Generaliza a
 * antiga entidade TCC (thesis) numa superficie unica de publicacao:
 * <ul>
 *   <li>{@code ANNOUNCEMENT} - comunicado (corpo textual).</li>
 *   <li>{@code ATTACHMENT}   - anexo/documento (PDF via file_url).</li>
 *   <li>{@code WORK}         - trabalho academico / TCC (autores, orientadores, ano).</li>
 * </ul>
 */
public enum ContentType {
    ANNOUNCEMENT,
    ATTACHMENT,
    WORK
}
