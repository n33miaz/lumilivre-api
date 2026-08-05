package br.com.lumilivre.api.security;

import java.time.OffsetDateTime;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import br.com.lumilivre.api.exception.custom.MessageKeyedException;
import br.com.lumilivre.api.model.AuditLog;
import lombok.RequiredArgsConstructor;

@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private static final Logger log = LoggerFactory.getLogger(AuditAspect.class);

    /** Teto do motivo gravado: a coluna é TEXT, a trilha não é lugar de dump. */
    private static final int MAX_REASON_LENGTH = 200;

    private final AuditLogWriter auditLogWriter;
    private final ClientIpResolver clientIpResolver;
    private final ExpressionParser spel = new SpelExpressionParser();

    @Around("@annotation(auditable)")
    public Object audit(ProceedingJoinPoint pjp, Auditable auditable) throws Throwable {
        String actor = resolveActor();
        String role = resolveRole();
        String action = auditable.action().isBlank()
                ? pjp.getSignature().getName().toUpperCase()
                : auditable.action();

        try {
            Object result = pjp.proceed();
            persist(actor, role, action, resolveTarget(pjp, auditable.targetParam(), result),
                    "SUCCESS", null, false);
            return result;
        } catch (Throwable t) {
            persist(actor, role, action, resolveTarget(pjp, auditable.targetParam(), null),
                    "FAILURE", describeFailure(t, action), true);
            throw t;
        }
    }

    private String resolveActor() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return "anonymous";
        Object principal = auth.getPrincipal();
        if (principal instanceof CustomUserDetails cud) {
            var reader = cud.getAppUser().getReader();
            return reader != null ? reader.getRegistrationNumber() : cud.getUsername();
        }
        return auth.getName();
    }

    private String resolveRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getAuthorities().isEmpty()) return "UNKNOWN";
        return auth.getAuthorities().iterator().next().getAuthority();
    }

    /**
     * Resolve o alvo por Spring EL sobre os parâmetros, com {@code #result}
     * também disponível: em criação o identificador só existe depois do save, e
     * sem {@code #result.id} sobrava usar um campo do request como alvo — o que,
     * para usuário, seria gravar o e-mail dele no {@code audit_log}.
     */
    private String resolveTarget(ProceedingJoinPoint pjp, String targetParam, Object result) {
        if (targetParam.isBlank()) return null;
        // Falha em método que aponta o alvo pelo retorno: não há alvo, e avaliar
        // renderia só um warning por requisição falhada.
        if (result == null && targetParam.contains("#result")) return null;
        try {
            MethodSignature sig = (MethodSignature) pjp.getSignature();
            String[] paramNames = sig.getParameterNames();
            Object[] args = pjp.getArgs();

            StandardEvaluationContext ctx = new StandardEvaluationContext();
            for (int i = 0; i < paramNames.length; i++) {
                ctx.setVariable(paramNames[i], args[i]);
            }
            ctx.setVariable("result", result);
            Object value = spel.parseExpression(targetParam).getValue(ctx);
            return value != null ? value.toString() : null;
        } catch (Exception e) {
            log.warn("AuditAspect: could not resolve targetParam '{}': {}", targetParam, e.getMessage());
            return null;
        }
    }

    /**
     * Motivo da falha em forma estável, e nunca a mensagem crua de exceção de
     * terceiro.
     *
     * <p>{@code getMessage()} de uma {@code DataIntegrityViolationException} traz
     * o SQL com os valores dos parâmetros — CPF, telefone e endereço do leitor
     * entrariam na tabela de auditoria por essa porta. Das nossas exceções
     * aproveitamos a chave i18n, que já é um identificador curto e estável
     * ({@code reader.cpf.already-registered}); de qualquer outra guardamos só o
     * nome da classe e mandamos o detalhe para o log, onde o correlationId
     * permite recuperá-lo.
     */
    private String describeFailure(Throwable t, String action) {
        if (t instanceof MessageKeyedException keyed && keyed.hasI18nKey()) {
            return truncate(keyed.getMessageKey());
        }
        if (t instanceof AccessDeniedException) {
            return "access-denied";
        }
        log.warn("AuditAspect: {} failed with a foreign exception [correlationId={}]: {}",
                action, MDC.get("correlationId"), t.toString());
        return t.getClass().getSimpleName();
    }

    private static String truncate(String value) {
        if (value == null) return null;
        return value.length() <= MAX_REASON_LENGTH ? value : value.substring(0, MAX_REASON_LENGTH);
    }

    private void persist(String actor, String role, String action, String targetId,
                         String result, String errorMessage, boolean ownTransaction) {
        AuditLog auditLog = AuditLog.builder()
                .actor(actor)
                .actorRole(role)
                .action(action)
                .targetId(targetId)
                .result(result)
                .errorMessage(errorMessage)
                .ipAddress(clientIpResolver.resolveCurrent())
                .occurredAt(OffsetDateTime.now())
                .build();
        try {
            if (ownTransaction) {
                auditLogWriter.writeInNewTransaction(auditLog);
            } else {
                auditLogWriter.writeInCallerTransaction(auditLog);
            }
        } catch (Exception e) {
            log.error("AuditAspect: failed to persist audit log for action={}: {}", action, e.getMessage());
        }
    }
}
