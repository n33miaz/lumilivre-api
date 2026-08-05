package br.com.lumilivre.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import br.com.lumilivre.api.enums.AccessEvent;
import br.com.lumilivre.api.enums.Role;
import br.com.lumilivre.api.model.AccessLog;
import br.com.lumilivre.api.model.AppUser;
import br.com.lumilivre.api.model.Reader;
import br.com.lumilivre.api.repository.AccessLogRepository;
import br.com.lumilivre.api.security.ClientIpResolver;
import br.com.lumilivre.api.security.CustomUserDetails;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

/**
 * O que separa auditoria de acessos de log de servidor: ator identificado,
 * dedupe por janela e alvo do evento.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AccessLogServiceTest {

    @Mock
    private AccessLogRepository accessLogRepository;

    @Mock
    private ClientIpResolver clientIpResolver;

    private SimpleMeterRegistry registry;
    private AccessLogService service;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        service = new AccessLogService(accessLogRepository, clientIpResolver, registry);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Client", "APP");
        request.addHeader("User-Agent", "Dart/3.9 (dart:io) lumilivre");
        request.setRemoteAddr("200.150.10.22");
        when(clientIpResolver.currentRequest()).thenReturn(request);
        when(clientIpResolver.resolve(any())).thenReturn("200.150.10.22");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void usoDeLeitorGravaCanalIpEAlvo() {
        authenticateReader("2024001");

        service.recordUsage(AccessEvent.BOOK_VIEWED, "book-42");

        AccessLog saved = captureSaved();
        assertThat(saved.getActor()).isEqualTo("2024001");
        assertThat(saved.getActorRole()).isEqualTo("ROLE_READER");
        assertThat(saved.getEvent()).isEqualTo("BOOK_VIEWED");
        assertThat(saved.getChannel()).isEqualTo("APP");
        assertThat(saved.getIpAddress()).isEqualTo("200.150.10.22");
        assertThat(saved.getTargetId()).isEqualTo("book-42");
        assertThat(saved.getResult()).isEqualTo(AccessLogService.RESULT_SUCCESS);
    }

    @Test
    void usoAnonimoNaoGravaLinhaMasContaNaMetrica() {
        // A ficha do livro e o catálogo são anônimos. Se uso anônimo gravasse
        // linha, qualquer varredura escreveria no banco por nós — e a linha não
        // responderia a pergunta nenhuma, porque não tem ator.
        SecurityContextHolder.getContext().setAuthentication(
                new AnonymousAuthenticationToken("key", "anonymousUser",
                        List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));

        service.recordUsage(AccessEvent.BOOK_VIEWED, "book-42");

        verify(accessLogRepository, never()).save(any());
        assertThat(registry.get("access.usage.anonymous").counter().count()).isEqualTo(1.0);
    }

    @Test
    void semAutenticacaoAlgumaTambemNaoGrava() {
        service.recordUsage(AccessEvent.CATALOG_SEARCH, null);

        verify(accessLogRepository, never()).save(any());
    }

    @Test
    void repeticaoNaMesmaJanelaGravaUmaVezSo() {
        // A tela do catálogo dispara várias chamadas ao abrir; a auditoria
        // registra a intenção do aluno, não cada requisição do cliente.
        authenticateReader("2024001");

        service.recordUsage(AccessEvent.CATALOG_SEARCH, null);
        service.recordUsage(AccessEvent.CATALOG_SEARCH, null);
        service.recordUsage(AccessEvent.CATALOG_SEARCH, null);

        verify(accessLogRepository).save(any());
    }

    @Test
    void alvosDiferentesSaoEventosDiferentes() {
        authenticateReader("2024001");

        service.recordUsage(AccessEvent.BOOK_VIEWED, "book-1");
        service.recordUsage(AccessEvent.BOOK_VIEWED, "book-2");

        verify(accessLogRepository, org.mockito.Mockito.times(2)).save(any());
    }

    @Test
    void leitoresDiferentesNaoCompartilhamAJanela() {
        authenticateReader("2024001");
        service.recordUsage(AccessEvent.CATALOG_SEARCH, null);

        authenticateReader("2024002");
        service.recordUsage(AccessEvent.CATALOG_SEARCH, null);

        verify(accessLogRepository, org.mockito.Mockito.times(2)).save(any());
    }

    @Test
    void falhaDePersistenciaNaoDerrubaARequisicao() {
        authenticateReader("2024001");
        when(accessLogRepository.save(any())).thenThrow(new RuntimeException("banco fora"));

        service.recordUsage(AccessEvent.BOOK_VIEWED, "book-42");
        // Sem exceção propagada: auditoria não troca um 200 por um 500.
    }

    @Test
    void eventoDeAutenticacaoContinuaSemAlvo() {
        service.record(AccessEvent.LOGIN, "admin", "ROLE_ADMIN", AccessLogService.RESULT_SUCCESS, null);

        assertThat(captureSaved().getTargetId()).isNull();
    }

    private AccessLog captureSaved() {
        ArgumentCaptor<AccessLog> captor = ArgumentCaptor.forClass(AccessLog.class);
        verify(accessLogRepository).save(captor.capture());
        return captor.getValue();
    }

    private static void authenticateReader(String registrationNumber) {
        Reader reader = new Reader();
        reader.setRegistrationNumber(registrationNumber);
        AppUser appUser = AppUser.builder()
                .email(registrationNumber + "@lumilivre.test")
                .passwordHash("hash")
                .role(Role.READER)
                .reader(reader)
                .build();
        CustomUserDetails principal = new CustomUserDetails(appUser);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }
}
