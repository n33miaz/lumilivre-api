package br.com.lumilivre.api.security;

import java.time.OffsetDateTime;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import br.com.lumilivre.api.model.AuditLog;
import br.com.lumilivre.api.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;

@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private static final Logger log = LoggerFactory.getLogger(AuditAspect.class);

    private final AuditLogRepository auditLogRepository;
    private final ExpressionParser spel = new SpelExpressionParser();

    @Around("@annotation(auditable)")
    public Object audit(ProceedingJoinPoint pjp, Auditable auditable) throws Throwable {
        String actor = resolveActor();
        String role = resolveRole();
        String action = auditable.action().isBlank()
                ? pjp.getSignature().getName().toUpperCase()
                : auditable.action();
        String targetId = resolveTarget(pjp, auditable.targetParam());

        try {
            Object result = pjp.proceed();
            persist(actor, role, action, targetId, "SUCCESS", null);
            return result;
        } catch (Throwable t) {
            persist(actor, role, action, targetId, "FAILURE", t.getMessage());
            throw t;
        }
    }

    private String resolveActor() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return "anonymous";
        Object principal = auth.getPrincipal();
        if (principal instanceof CustomUserDetails cud) {
            var student = cud.getAppUser().getStudent();
            return student != null ? student.getRegistrationNumber() : cud.getUsername();
        }
        return auth.getName();
    }

    private String resolveRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getAuthorities().isEmpty()) return "UNKNOWN";
        return auth.getAuthorities().iterator().next().getAuthority();
    }

    private String resolveTarget(ProceedingJoinPoint pjp, String targetParam) {
        if (targetParam.isBlank()) return null;
        try {
            MethodSignature sig = (MethodSignature) pjp.getSignature();
            String[] paramNames = sig.getParameterNames();
            Object[] args = pjp.getArgs();

            StandardEvaluationContext ctx = new StandardEvaluationContext();
            for (int i = 0; i < paramNames.length; i++) {
                ctx.setVariable(paramNames[i], args[i]);
            }
            Object value = spel.parseExpression(targetParam).getValue(ctx);
            return value != null ? value.toString() : null;
        } catch (Exception e) {
            log.warn("AuditAspect: could not resolve targetParam '{}': {}", targetParam, e.getMessage());
            return null;
        }
    }

    private void persist(String actor, String role, String action, String targetId,
                         String result, String errorMessage) {
        try {
            auditLogRepository.save(
                    AuditLog.builder()
                            .actor(actor)
                            .actorRole(role)
                            .action(action)
                            .targetId(targetId)
                            .result(result)
                            .errorMessage(errorMessage)
                            .occurredAt(OffsetDateTime.now())
                            .build()
            );
        } catch (Exception e) {
            log.error("AuditAspect: failed to persist audit log for action={}: {}", action, e.getMessage());
        }
    }
}
