package br.com.lumilivre.api.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Garante que o chamador é ADMIN, BIBLIOTECARIO ou o próprio aluno
 * dono da {matricula} presente no path variable.
 *
 * Uso: @CanAccessStudent no método do controller que recebe {matricula}.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("@studentAuthz.canAccess(#matricula)")
public @interface CanAccessStudent {
}
