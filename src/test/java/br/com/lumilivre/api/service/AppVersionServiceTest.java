package br.com.lumilivre.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import br.com.lumilivre.api.dto.appversion.AppVersionRequest;
import br.com.lumilivre.api.dto.appversion.AppVersionResponse;
import br.com.lumilivre.api.enums.Role;
import br.com.lumilivre.api.exception.custom.BusinessRuleException;
import br.com.lumilivre.api.exception.custom.ResourceNotFoundException;
import br.com.lumilivre.api.model.AppUser;
import br.com.lumilivre.api.model.AppVersion;
import br.com.lumilivre.api.repository.AppVersionRepository;
import br.com.lumilivre.api.security.CustomUserDetails;

/**
 * O portão de versão mínima é o único endpoint que o app consulta <b>antes</b>
 * do login: ele é público por necessidade, e por isso o que ele devolve a um
 * anônimo é decisão de segurança (SEC-NEW-02) e não detalhe de serialização.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AppVersionServiceTest {

    private static final OffsetDateTime PUBLICADO_EM = OffsetDateTime.parse("2026-02-10T09:30:00Z");

    @Mock
    private AppVersionRepository repository;

    private AppVersionService service;

    @BeforeEach
    void setUp() {
        service = new AppVersionService(repository);
        when(repository.save(any(AppVersion.class))).thenAnswer(i -> i.getArgument(0));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    /**
     * O nome do administrador que publicou a versão e a hora em que publicou não
     * têm uso para o app e identificam uma pessoa da escola. Como a rota é
     * anônima, qualquer um na internet leria os dois com um GET.
     */
    @Test
    void oAnonimoNaoDescobreQuemPublicouAVersao() {
        when(repository.findById("ANDROID")).thenReturn(Optional.of(androidPublicado()));
        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken(
                "key", "anonymousUser", java.util.List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));

        AppVersionResponse resposta = service.get("ANDROID");

        assertThat(resposta.updatedBy()).isNull();
        assertThat(resposta.updatedAt()).isNull();
        // O que o app precisa continua saindo inteiro.
        assertThat(resposta.minSupportedBuild()).isEqualTo(40);
        assertThat(resposta.forceUpdate()).isTrue();
        assertThat(resposta.updateMessage()).isEqualTo("Atualize para continuar usando.");
    }

    @Test
    void semAutenticacaoAlgumaTambemNaoSaiAAuditoria() {
        when(repository.findById("ANDROID")).thenReturn(Optional.of(androidPublicado()));

        assertThat(service.get("ANDROID").updatedBy()).isNull();
    }

    /** O painel admin mostra quem mexeu por último — ali o campo tem dono e uso. */
    @Test
    void oPainelAutenticadoEnxergaAAuditoria() {
        when(repository.findById("ANDROID")).thenReturn(Optional.of(androidPublicado()));
        autenticar(Role.ADMIN);

        AppVersionResponse resposta = service.get("ANDROID");

        assertThat(resposta.updatedBy()).isEqualTo("admin@lumilivre.test");
        assertThat(resposta.updatedAt()).isEqualTo(PUBLICADO_EM);
    }

    @ParameterizedTest
    @ValueSource(strings = {"android", "  Android  ", "ANDROID"})
    void aPlataformaChegaComoOAppMandaESaiNormalizada(String informada) {
        when(repository.findById("ANDROID")).thenReturn(Optional.of(androidPublicado()));

        assertThat(service.get(informada).platform()).isEqualTo("ANDROID");
    }

    /**
     * Plataforma desconhecida é 400 e não 404: o app mandou algo que não existe
     * no contrato, e um 404 faria o cliente concluir "ainda não configuraram
     * essa plataforma" — e seguir sem o portão de versão.
     */
    @ParameterizedTest
    @ValueSource(strings = {"windows", "web", "ANDROI", ""})
    void plataformaForaDoContratoEErroDeRequisicao(String invalida) {
        assertThatExceptionOfType(BusinessRuleException.class)
                .isThrownBy(() -> service.get(invalida))
                .withMessageContaining("app-version.platform.invalid");
    }

    @Test
    void plataformaNulaTambemEErroDeRequisicaoENaoNullPointer() {
        assertThatExceptionOfType(BusinessRuleException.class)
                .isThrownBy(() -> service.get(null));
    }

    @Test
    void plataformaValidaSemLinhaNoBancoENaoEncontrada() {
        when(repository.findById("IOS")).thenReturn(Optional.empty());

        assertThatExceptionOfType(ResourceNotFoundException.class)
                .isThrownBy(() -> service.get("IOS"));
    }

    /**
     * Uma linha por plataforma, duas URLs de loja: o app tem que receber a
     * <i>sua</i>. Mandar a da Play Store para um iPhone deixa o botão "atualizar"
     * apontando para o vazio justamente quando o app está bloqueado.
     */
    @Test
    void cadaPlataformaRecebeAUrlDaPropriaLoja() {
        AppVersion android = androidPublicado();
        when(repository.findById("ANDROID")).thenReturn(Optional.of(android));

        AppVersion ios = androidPublicado();
        ios.setPlatform("IOS");
        when(repository.findById("IOS")).thenReturn(Optional.of(ios));

        assertThat(service.get("ANDROID").storeUrl()).isEqualTo("https://play.google.com/store/apps/lumilivre");
        assertThat(service.get("IOS").storeUrl()).isEqualTo("https://apps.apple.com/app/lumilivre");
    }

    /**
     * {@code forceUpdate} nulo vira {@code false}. O default tem que ser "não
     * bloqueia": uma linha incompleta no banco não pode derrubar o app de todo
     * mundo por omissão.
     */
    @Test
    void semForceUpdateOAppNaoEBloqueado() {
        AppVersion semFlag = androidPublicado();
        semFlag.setForceUpdate(null);
        when(repository.findById("ANDROID")).thenReturn(Optional.of(semFlag));

        assertThat(service.get("ANDROID").forceUpdate()).isFalse();
    }

    // ---- publicação ----------------------------------------------------------

    @Test
    void publicarUmaPlataformaNovaCriaALinha() {
        when(repository.findById("IOS")).thenReturn(Optional.empty());

        AppVersionResponse resposta = service.update(requisicao("ios"), "admin@lumilivre.test");

        assertThat(resposta.platform()).isEqualTo("IOS");
        assertThat(resposta.latestBuild()).isEqualTo(51);
        assertThat(resposta.minSupportedBuild()).isEqualTo(45);
        assertThat(resposta.updatedBy()).isEqualTo("admin@lumilivre.test");
        assertThat(resposta.updatedAt()).isNotNull();
    }

    @Test
    void publicarSobreumaLinhaExistenteTrocaOsCamposEOAutor() {
        AppVersion existente = androidPublicado();
        when(repository.findById("ANDROID")).thenReturn(Optional.of(existente));

        AppVersionResponse resposta = service.update(requisicao("ANDROID"), "outro@lumilivre.test");

        assertThat(resposta.latestVersion()).isEqualTo("2.1.0");
        assertThat(resposta.updatedBy()).isEqualTo("outro@lumilivre.test");
        // O carimbo antigo é preservado aqui; quem o move é o @PreUpdate do JPA.
        assertThat(existente.getUpdatedAt()).isEqualTo(PUBLICADO_EM);
    }

    /** O painel sempre vê a auditoria da resposta de escrita, autenticado ou não. */
    @Test
    void aRespostaDaPublicacaoSempreCarregaAAuditoria() {
        when(repository.findById("ANDROID")).thenReturn(Optional.of(androidPublicado()));

        assertThat(service.update(requisicao("ANDROID"), "admin@lumilivre.test").updatedBy())
                .isEqualTo("admin@lumilivre.test");
    }

    @Test
    void publicarComPlataformaForaDoContratoERecusado() {
        assertThatExceptionOfType(BusinessRuleException.class)
                .isThrownBy(() -> service.update(requisicao("symbian"), "admin@lumilivre.test"));
    }

    // ---- fixtures ------------------------------------------------------------

    private static AppVersionRequest requisicao(String plataforma) {
        return new AppVersionRequest(plataforma, "2.1.0", 51, "2.0.0", 45, true,
                "Atualize para continuar usando.",
                "https://play.google.com/store/apps/lumilivre",
                "https://apps.apple.com/app/lumilivre");
    }

    private static AppVersion androidPublicado() {
        return AppVersion.builder()
                .platform("ANDROID")
                .latestVersion("2.0.0")
                .latestBuild(50)
                .minSupportedVersion("1.9.0")
                .minSupportedBuild(40)
                .forceUpdate(true)
                .updateMessage("Atualize para continuar usando.")
                .storeUrlAndroid("https://play.google.com/store/apps/lumilivre")
                .storeUrlIos("https://apps.apple.com/app/lumilivre")
                .updatedAt(PUBLICADO_EM)
                .updatedBy("admin@lumilivre.test")
                .build();
    }

    private static void autenticar(Role role) {
        AppUser appUser = AppUser.builder()
                .email("admin@lumilivre.test")
                .passwordHash("hash")
                .role(role)
                .build();
        CustomUserDetails principal = new CustomUserDetails(appUser);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }
}
