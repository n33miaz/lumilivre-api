package br.com.lumilivre.api.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.UUID;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import br.com.lumilivre.api.exception.custom.BusinessRuleException;
import br.com.lumilivre.api.model.AuditLog;

@ExtendWith(MockitoExtension.class)
class AuditAspectTest {

    @Mock
    private AuditLogWriter auditLogWriter;

    @Mock
    private ClientIpResolver clientIpResolver;

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
        verify(auditLogWriter).writeInCallerTransaction(captor.capture());
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
    void falhaVaiParaTransacaoPropriaParaSobreviverAoRollback() throws Throwable {
        // O defeito corrigido: o aspecto gravava na transação do método auditado,
        // então a linha de FAILURE nascia marcada para rollback junto com a regra
        // de negócio que falhou e desaparecia. A trilha guardava só o que deu
        // certo — o oposto do que uma revisão de segurança procura.
        authenticate("bibliotecario@lumilivre.test", "ROLE_LIBRARIAN");
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getParameterNames()).thenReturn(new String[] { "matricula" });
        when(joinPoint.getArgs()).thenReturn(new Object[] { "12345" });
        when(joinPoint.proceed())
                .thenThrow(BusinessRuleException.ofKey("reader.cpf.already-registered"));

        assertThatThrownBy(() -> auditAspect.audit(joinPoint, auditable("READER_UPDATED", "#matricula")))
                .isInstanceOf(BusinessRuleException.class);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogWriter).writeInNewTransaction(captor.capture());

        assertThat(captor.getValue().getResult()).isEqualTo("FAILURE");
        assertThat(captor.getValue().getTargetId()).isEqualTo("12345");
        // Motivo estável: a chave i18n, não texto localizado nem stack trace.
        assertThat(captor.getValue().getErrorMessage()).isEqualTo("reader.cpf.already-registered");
    }

    @Test
    void mensagemDeExcecaoEstranhaNaoEntraNaTrilha() throws Throwable {
        // getMessage() de uma violação de integridade traz o SQL com os valores
        // dos parâmetros: CPF, telefone e endereço do leitor entrariam no
        // audit_log por essa porta.
        authenticate("admin@lumilivre.test", "ROLE_ADMIN");
        when(joinPoint.proceed()).thenThrow(new DataIntegrityViolationException(
                "insert into reader (cpf, phone) values ('529.982.247-25', '11999998888')"));

        assertThatThrownBy(() -> auditAspect.audit(joinPoint, auditable("READER_CREATED", "")))
                .isInstanceOf(DataIntegrityViolationException.class);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogWriter).writeInNewTransaction(captor.capture());

        assertThat(captor.getValue().getErrorMessage())
                .isEqualTo("DataIntegrityViolationException")
                .doesNotContain("529.982.247-25")
                .doesNotContain("11999998888");
    }

    @Test
    void acessoNegadoEGravadoComMotivoFixo() throws Throwable {
        authenticate("2024001", "ROLE_READER");
        when(joinPoint.proceed()).thenThrow(new AccessDeniedException("Access is denied"));

        assertThatThrownBy(() -> auditAspect.audit(joinPoint, auditable("READER_UPDATED", "")))
                .isInstanceOf(AccessDeniedException.class);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogWriter).writeInNewTransaction(captor.capture());

        assertThat(captor.getValue().getErrorMessage()).isEqualTo("access-denied");
    }

    @Test
    void alvoPodeVirDoRetornoQuandoOIdSoExisteDepoisDoSave() throws Throwable {
        // Em criação não há id nos parâmetros. Sem #result sobrava usar um campo
        // do request como alvo — para usuário, isso seria gravar o e-mail dele.
        authenticate("admin@lumilivre.test", "ROLE_ADMIN");
        UUID generated = UUID.fromString("11111111-2222-3333-4444-555555555555");
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getParameterNames()).thenReturn(new String[] { "request" });
        when(joinPoint.getArgs()).thenReturn(new Object[] { "ignorado" });
        when(joinPoint.proceed()).thenReturn(new CreatedEntity(generated));

        auditAspect.audit(joinPoint, auditable("USER_CREATED", "#result.id"));

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogWriter).writeInCallerTransaction(captor.capture());

        assertThat(captor.getValue().getTargetId()).isEqualTo(generated.toString());
    }

    @Test
    void alvoPeloRetornoFicaNuloQuandoACriacaoFalha() throws Throwable {
        authenticate("admin@lumilivre.test", "ROLE_ADMIN");
        when(joinPoint.proceed()).thenThrow(BusinessRuleException.ofKey("user.email.in-use"));

        assertThatThrownBy(() -> auditAspect.audit(joinPoint, auditable("USER_CREATED", "#result.id")))
                .isInstanceOf(BusinessRuleException.class);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogWriter).writeInNewTransaction(captor.capture());

        assertThat(captor.getValue().getTargetId()).isNull();
        assertThat(captor.getValue().getResult()).isEqualTo("FAILURE");
    }

    @Test
    void auditDeveUsarAnonymousQuandoNaoHaAutenticacao() throws Throwable {
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn("cancelar");
        when(joinPoint.proceed()).thenReturn(null);

        auditAspect.audit(joinPoint, auditable("", ""));

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogWriter).writeInCallerTransaction(captor.capture());

        assertThat(captor.getValue().getActor()).isEqualTo("anonymous");
        assertThat(captor.getValue().getActorRole()).isEqualTo("UNKNOWN");
        assertThat(captor.getValue().getAction()).isEqualTo("CANCELAR");
        assertThat(captor.getValue().getTargetId()).isNull();
    }

    private record CreatedEntity(UUID id) {}

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
