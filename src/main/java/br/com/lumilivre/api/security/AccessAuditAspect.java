package br.com.lumilivre.api.security;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import br.com.lumilivre.api.service.AccessLogService;
import lombok.RequiredArgsConstructor;

/**
 * Grava o evento de uso de um endpoint anotado com {@link AccessAudited}.
 *
 * <p>Só grava <b>depois</b> de a chamada devolver com sucesso: uma ficha de
 * livro que respondeu 404 não é "o aluno consultou o acervo", e um 403 já está
 * na trilha como {@code ACCESS_DENIED} pelo handler do
 * {@code SecurityConfig}. Exceção segue intacta para o
 * {@code GlobalExceptionHandler}.
 *
 * <p>Separado do {@link AuditAspect} porque as duas trilhas respondem perguntas
 * diferentes e têm volumes diferentes: {@code audit_log} guarda quem <i>mudou</i>
 * o acervo (poucas linhas, todas relevantes uma a uma), {@code access_log} guarda
 * quem <i>usou</i> a biblioteca (muitas linhas, relevantes em agregado).
 */
@Aspect
@Component
@RequiredArgsConstructor
public class AccessAuditAspect {

    private static final Logger log = LoggerFactory.getLogger(AccessAuditAspect.class);

    private final AccessLogService accessLogService;
    private final ExpressionParser spel = new SpelExpressionParser();

    @Around("@annotation(accessAudited)")
    public Object audit(ProceedingJoinPoint pjp, AccessAudited accessAudited) throws Throwable {
        Object result = pjp.proceed();
        try {
            accessLogService.recordUsage(accessAudited.event(), resolveTarget(pjp, accessAudited.targetParam()));
        } catch (Exception e) {
            // Auditoria nunca troca um 200 por um 500.
            log.warn("AccessAuditAspect: could not record usage event {}: {}",
                    accessAudited.event(), e.getMessage());
        }
        return result;
    }

    private String resolveTarget(ProceedingJoinPoint pjp, String targetParam) {
        if (targetParam.isBlank()) {
            return null;
        }
        try {
            MethodSignature signature = (MethodSignature) pjp.getSignature();
            String[] paramNames = signature.getParameterNames();
            Object[] args = pjp.getArgs();

            StandardEvaluationContext ctx = new StandardEvaluationContext();
            for (int i = 0; i < paramNames.length; i++) {
                ctx.setVariable(paramNames[i], args[i]);
            }
            Object value = spel.parseExpression(targetParam).getValue(ctx);
            return value != null ? value.toString() : null;
        } catch (Exception e) {
            log.warn("AccessAuditAspect: could not resolve targetParam '{}': {}", targetParam, e.getMessage());
            return null;
        }
    }
}
