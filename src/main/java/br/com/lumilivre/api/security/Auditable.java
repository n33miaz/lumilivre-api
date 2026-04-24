package br.com.lumilivre.api.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marca um método de serviço para auditoria automática via AOP.
 * O aspect persiste um AuditLog após cada invocação, com resultado SUCCESS ou FAILURE.
 *
 * @param action Nome da ação auditada (ex: "LOAN_CREATED"). Padrão: nome do método.
 * @param targetParam Nome do parâmetro Spring EL que identifica o recurso (ex: "#matricula", "#id").
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Auditable {
    String action() default "";
    String targetParam() default "";
}
