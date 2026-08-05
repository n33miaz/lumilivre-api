package br.com.lumilivre.api.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.annotation.Annotation;
import java.util.UUID;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.lumilivre.api.enums.AccessEvent;
import br.com.lumilivre.api.exception.custom.ResourceNotFoundException;
import br.com.lumilivre.api.service.AccessLogService;

@ExtendWith(MockitoExtension.class)
class AccessAuditAspectTest {

    @Mock
    private AccessLogService accessLogService;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private MethodSignature signature;

    @InjectMocks
    private AccessAuditAspect aspect;

    @Test
    void gravaEventoComAlvoResolvidoDoParametro() throws Throwable {
        UUID bookId = UUID.randomUUID();
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getParameterNames()).thenReturn(new String[] { "id", "locale" });
        when(joinPoint.getArgs()).thenReturn(new Object[] { bookId, null });
        when(joinPoint.proceed()).thenReturn("ficha");

        Object result = aspect.audit(joinPoint, audited(AccessEvent.BOOK_VIEWED, "#id"));

        assertThat(result).isEqualTo("ficha");
        verify(accessLogService).recordUsage(AccessEvent.BOOK_VIEWED, bookId.toString());
    }

    @Test
    void eventoSemAlvoGravaAlvoNulo() throws Throwable {
        when(joinPoint.proceed()).thenReturn("catalogo");

        aspect.audit(joinPoint, audited(AccessEvent.CATALOG_SEARCH, ""));

        verify(accessLogService).recordUsage(AccessEvent.CATALOG_SEARCH, null);
    }

    @Test
    void chamadaQueFalhaNaoEntraNaTrilhaDeUso() throws Throwable {
        // Ficha que respondeu 404 não é "o aluno consultou o acervo"; e um 403 já
        // vira ACCESS_DENIED pelo handler do SecurityConfig.
        when(joinPoint.proceed()).thenThrow(ResourceNotFoundException.ofKey("book.not-found"));

        assertThatThrownBy(() -> aspect.audit(joinPoint, audited(AccessEvent.BOOK_VIEWED, "#id")))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(accessLogService, never()).recordUsage(any(), any());
    }

    @Test
    void falhaDaAuditoriaNaoTrocaResposta() throws Throwable {
        when(joinPoint.proceed()).thenReturn("catalogo");
        doThrow(new RuntimeException("banco fora"))
                .when(accessLogService).recordUsage(eq(AccessEvent.CATALOG_SEARCH), any());

        assertThat(aspect.audit(joinPoint, audited(AccessEvent.CATALOG_SEARCH, ""))).isEqualTo("catalogo");
    }

    private static AccessAudited audited(AccessEvent event, String targetParam) {
        return new AccessAudited() {
            @Override
            public AccessEvent event() {
                return event;
            }

            @Override
            public String targetParam() {
                return targetParam;
            }

            @Override
            public Class<? extends Annotation> annotationType() {
                return AccessAudited.class;
            }
        };
    }
}
