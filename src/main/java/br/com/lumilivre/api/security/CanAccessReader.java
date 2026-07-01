package br.com.lumilivre.api.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Garante que o chamador é ADMIN, BIBLIOTECARIO ou o próprio leitor
 * dono da {registrationNumber} presente no path variable.
 *
 * Uso: @CanAccessReader no método do controller que recebe {registrationNumber}.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("@readerAuthz.canAccess(#registrationNumber)")
public @interface CanAccessReader {
}
