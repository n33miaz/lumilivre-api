package br.com.lumilivre.api.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Autoriza ADMIN/BIBLIOTECARIO ou o aluno dono do emprestimo informado por {id}.
 */
@Target({ ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("@studentAuthz.canAccessLoan(#id)")
public @interface CanAccessLoan {
}
