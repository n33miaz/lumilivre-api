package br.com.lumilivre.api.enums;

/**
 * Tipos de evento de acesso registrados em {@link br.com.lumilivre.api.model.AccessLog}.
 */
public enum AccessEvent {
    LOGIN,
    LOGOUT,
    LOGIN_FAILED,
    TOKEN_REFRESH,
    ACCESS_DENIED
}
