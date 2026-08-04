package br.com.lumilivre.api.enums;

/**
 * Segmentacao de publico de {@link br.com.lumilivre.api.model.AppContent}. Define
 * quais leitores enxergam o conteudo no mural do app:
 * <ul>
 *   <li>{@code ALL}    - todos os leitores.</li>
 *   <li>{@code COURSE} - apenas o curso indicado ({@code course_id}).</li>
 *   <li>{@code MODULE} - apenas o modulo indicado ({@code academic_module_id}).</li>
 *   <li>{@code SHIFT}  - apenas o turno indicado ({@code study_shift_id}).</li>
 * </ul>
 */
public enum AudienceScope {
    ALL,
    COURSE,
    MODULE,
    SHIFT
}
