package br.com.lumilivre.api.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.annotation.Annotation;
import java.util.List;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import br.com.lumilivre.api.model.AuditLog;
import br.com.lumilivre.api.repository.AuditLogRepository;

@ExtendWith(MockitoExtension.class)
class AuditAspectTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private MethodSignature signature;

    @InjectMocks
    private AuditAspect auditAspect;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void auditDevePersistirSucessoComActorRoleETarget() throws Throwable {
        authenticate("admin@lumilivre.test", "ROLE_ADMIN");
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getParameterNames()).thenReturn(new String[] { "id" });
        when(joinPoint.getArgs()).thenReturn(new Object[] { 42 });
        when(joinPoint.proceed()).thenReturn("ok");

        Object result = auditAspect.audit(joinPoint, auditable("BOOK_UPDATED", "#id"));

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        AuditLog audit = captor.getValue();

        assertThat(result).isEqualTo("ok");
        assertThat(audit.getActor()).isEqualTo("admin@lumilivre.test");
        assertThat(audit.getActorRole()).isEqualTo("ROLE_ADMIN");
        assertThat(audit.getAction()).isEqualTo("BOOK_UPDATED");
        assertThat(audit.getTargetId()).isEqualTo("42");
        assertThat(audit.getResult()).isEqualTo("SUCCESS");
        assertThat(audit.getErrorMessage()).isNull();
        assertThat(audit.getOccurredAt()).isNotNull();
    }

    @Test
    void auditDevePersistirFalhaERelancarExcecao() throws Throwable {
        authenticate("bibliotecario@lumilivre.test", "ROLE_LIBRARIAN");
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getParameterNames()).thenReturn(new String[] { "matricula" });
        when(joinPoint.getArgs()).thenReturn(new Object[] { "12345" });
        when(joinPoint.proceed()).thenThrow(new IllegalStateException("falha controlada"));

        assertThatThrownBy(() -> auditAspect.audit(joinPoint, auditable("STUDENT_UPDATED", "#matricula")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("falha controlada");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        assertThat(captor.getValue().getResult()).isEqualTo("FAILURE");
        assertThat(captor.getValue().getErrorMessage()).isEqualTo("falha controlada");
        assertThat(captor.getValue().getTargetId()).isEqualTo("12345");
    }

    @Test
    void auditDeveUsarAnonymousQuandoNaoHaAutenticacao() throws Throwable {
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn("cancelar");
        when(joinPoint.proceed()).thenReturn(null);

        auditAspect.audit(joinPoint, auditable("", ""));

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        assertThat(captor.getValue().getActor()).isEqualTo("anonymous");
        assertThat(captor.getValue().getActorRole()).isEqualTo("UNKNOWN");
        assertThat(captor.getValue().getAction()).isEqualTo("CANCELAR");
        assertThat(captor.getValue().getTargetId()).isNull();
    }

    private static void authenticate(String username, String role) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        username,
                        "password",
                        List.of(new SimpleGrantedAuthority(role))));
    }

    private static Auditable auditable(String action, String targetParam) {
        return new Auditable() {
            @Override
            public String action() {
                return action;
            }

            @Override
            public String targetParam() {
                return targetParam;
            }

            @Override
            public Class<? extends Annotation> annotationType() {
                return Auditable.class;
            }
        };
    }
}
