package br.com.lumilivre.api.service.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Locale;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import br.com.lumilivre.api.config.EmailBrandingProperties;
import br.com.lumilivre.api.config.MessageResolver;
import br.com.lumilivre.api.service.infra.EmailService.RenderedEmail;
import br.com.lumilivre.api.service.infra.email.EmailTemplate;
import jakarta.mail.internet.MimeMessage;

/**
 * O corpo de cada e-mail transacional: se o dado certo chega, se o dado do
 * usuário chega escapado e se o link de redefinição continua clicável.
 *
 * <p>A última é a regra que já custou caro: o template genérico escapa o corpo
 * inteiro, então mandar a recuperação de senha por ele transformaria o link num
 * texto morto — e o usuário ficaria sem como voltar para a conta.
 */
@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class EmailServiceTest {

    private static final Locale PT = Locale.forLanguageTag("pt-BR");
    private static final Locale EN = Locale.forLanguageTag("en-US");
    private static final String LINK =
            "https://www.lumilivre.com.br/reset?token=8f3a2c9e-7b21-4d6f-9a0c-1e2b3c4d5e6f";

    @Mock
    private JavaMailSender mailSender;

    private EmailBrandingProperties branding;
    private EmailService service;

    @BeforeEach
    void setUp() {
        branding = new EmailBrandingProperties();
        ReloadableResourceBundleMessageSource source = new ReloadableResourceBundleMessageSource();
        source.setBasenames("classpath:i18n/email/messages");
        source.setDefaultEncoding("UTF-8");
        source.setFallbackToSystemLocale(false);

        MessageResolver messages = new MessageResolver(source);
        service = new EmailService(mailSender, messages, new EmailTemplate(branding, messages), branding);
        ReflectionTestUtils.setField(service, "from", "contato.lumilivre@gmail.com");
        ReflectionTestUtils.setField(service, "site", "https://www.lumilivre.com.br");

        when(mailSender.createMimeMessage()).thenAnswer(i -> new MimeMessage((jakarta.mail.Session) null));
    }

    // ---- senha inicial -------------------------------------------------------

    @Test
    void aSenhaInicialSaiComOLoginEASenhaNoCorpo() {
        RenderedEmail email = service.buildSenhaInicial("ada@escola.edu.br", "Ada Lovelace", "Lm7$kP2aZ9", PT);

        assertThat(email.subject()).isEqualTo("Acesso ao Portal LumiLivre");
        assertThat(email.html())
                .contains("Olá <strong>Ada Lovelace</strong>,")
                .contains("ada@escola.edu.br")
                .contains("Lm7$kP2aZ9")
                .contains("href=\"https://www.lumilivre.com.br\"");
    }

    /**
     * O nome vem do cadastro que a secretaria digita. Ele é interpolado dentro de
     * uma mensagem que <b>tem</b> markup ({@code <strong>{0}</strong>}), então o
     * escape precisa acontecer antes da interpolação — é o único ponto em que um
     * dado de terceiro entra num trecho HTML legítimo.
     */
    @Test
    void umNomeComHtmlNaoViraMarkupNoEmail() {
        RenderedEmail email = service.buildSenhaInicial(
                "ada@escola.edu.br", "<script>alert(1)</script>", "Lm7$kP2aZ9", PT);

        assertThat(email.html())
                .doesNotContain("<script>")
                .contains("&lt;script&gt;alert(1)&lt;/script&gt;");
    }

    @Test
    void oIdiomaDoDestinatarioTrocaOAssuntoEOCorpo() {
        assertThat(service.buildSenhaInicial("ada@escola.edu.br", "Ada", "x", EN).subject())
                .isEqualTo("LumiLivre Portal Access");
        assertThat(service.buildSenhaInicial("ada@escola.edu.br", "Ada", "x", EN).html())
                .contains("Your account has been created successfully!");
    }

    /**
     * A prévia da caixa de entrada do e-mail da equipe é o corpo sem as tags: o
     * texto do bundle traz {@code <strong>} para destacar o papel, e a prévia é
     * texto puro — sairia "&lt;strong&gt;Bibliotecário&lt;/strong&gt;" na lista
     * de mensagens.
     */
    @Test
    void aPreviaDoEmailDaEquipeNaoLevaTagHtml() {
        RenderedEmail email = service.buildSenhaInicialAdmin(
                "equipe@escola.edu.br", "<strong>Bibliotecário</strong>", "Adm!n9xQ2w", PT);

        assertThat(email.preheader()).doesNotContain("<", ">");
        assertThat(email.html()).contains("equipe@escola.edu.br").contains("Adm!n9xQ2w");
    }

    // ---- recuperação de senha ------------------------------------------------

    @Test
    void oLinkDeRedefinicaoChegaClicavelNoBotaoENoTextoDeApoio() {
        RenderedEmail email = service.buildResetSenha(LINK, PT);

        assertThat(email.subject()).isEqualTo("Redefinição de Senha - LumiLivre");
        assertThat(email.html())
                .contains("href=\"" + LINK + "\"")
                .contains("Se o botão não funcionar, copie e cole este link no navegador:")
                .contains("Redefinir minha senha");
    }

    /**
     * A comparação que justifica o template dedicado: pelo genérico o mesmo link
     * sai escapado e sem {@code href} — texto morto no meio do parágrafo.
     */
    @Test
    void oTemplateGenericoNaoServiriaParaOLinkDeRedefinicao() {
        String generico = service.buildGenerico("Recuperação", LINK, PT).html();

        assertThat(generico).doesNotContain("href=\"" + LINK + "\"");
        assertThat(service.buildResetSenha(LINK, PT).html()).contains("href=\"" + LINK + "\"");
    }

    // ---- genérico (outbox) ---------------------------------------------------

    @Test
    void oCorpoGenericoPreservaAsQuebrasDeLinhaComoBr() {
        String html = service.buildGenerico("Empréstimo", "Primeira linha.\nSegunda linha.", PT).html();

        assertThat(html).contains("Primeira linha.<br/>Segunda linha.");
    }

    @Test
    void oCorpoGenericoEscapaOTextoQueVeioDoDado() {
        String html = service.buildGenerico("Empréstimo", "<img src=x onerror=alert(1)>", PT).html();

        assertThat(html).doesNotContain("<img src=x").contains("&lt;img src=x");
    }

    @Test
    void aNotificacaoDeEmprestimoNomeiaOLivro() {
        RenderedEmail email = service.buildNotificacaoEmprestimo(
                "email.loan-overdue.subject", "email.loan-overdue.body", "Clean Code", EN);

        assertThat(email.html()).contains("Clean Code");
        assertThat(email.subject()).isNotEqualTo("email.loan-overdue.subject");
    }

    // ---- locale efetivo ------------------------------------------------------

    /**
     * O job de vencimento e o outbox chamam sem locale. Cair para pt-BR é
     * decisão consciente: melhor o idioma da escola do que o do servidor, que em
     * container costuma ser o do sistema operacional.
     */
    @Test
    void semLocaleOEmailSaiEmPortugues() throws Exception {
        service.enviarEmailResetSenha("ada@escola.edu.br", LINK, null);

        assertThat(capturada().getSubject()).isEqualTo("Redefinição de Senha - LumiLivre");
    }

    @Test
    void oLocaleDoContextoValeQuandoOChamadorNaoInforma() throws Exception {
        // Logotipo externo deixa a mensagem em text/html puro, o que torna o
        // corpo legível sem desmontar o multipart.
        branding.setLogoUrl("https://cdn.lumilivre.com.br/logo.png");
        LocaleContextHolder.setLocale(EN);
        try {
            service.enviarEmail("ada@escola.edu.br", "Assunto", "Corpo");
            assertThat(capturada().getContent().toString()).contains("Go to the portal");
        } finally {
            LocaleContextHolder.resetLocaleContext();
        }
    }

    // ---- transporte ----------------------------------------------------------

    /**
     * Sem logotipo externo a mensagem vai multipart com a imagem embutida. Se o
     * helper fosse criado sem multipart, o {@code addInline} estouraria e o
     * e-mail inteiro se perderia no catch — silenciosamente.
     */
    @Test
    void aMensagemSaiComDestinatarioAssuntoRemetenteELogotipoEmbutido() throws Exception {
        MimeMessage mensagem = enviarECapturar();

        assertThat(mensagem.getAllRecipients()[0].toString()).isEqualTo("ada@escola.edu.br");
        assertThat(mensagem.getSubject()).isEqualTo("Acesso ao Portal LumiLivre");
        assertThat(mensagem.getFrom()[0].toString()).isEqualTo("contato.lumilivre@gmail.com");
        assertThat(mensagem.getDataHandler().getContentType()).startsWith("multipart/");
        assertThat(serializar(mensagem))
                .contains("Content-ID: <" + EmailTemplate.LOGO_CID + ">")
                .contains("image/png");
    }

    /** Com logotipo hospedado fora não há anexo: a mensagem é só HTML. */
    @Test
    void comLogotipoExternoAMensagemNaoCarregaAnexo() throws Exception {
        branding.setLogoUrl("https://cdn.lumilivre.com.br/logo.png");

        assertThat(enviarECapturar().getDataHandler().getContentType()).startsWith("text/html");
    }

    @Test
    void oEnvioAcontecePelaSobrecargaSemLocaleTambem() {
        assertThatCode(() -> service.enviarSenhaInicial("ada@escola.edu.br", "Ada", "Lm7$kP2aZ9"))
                .doesNotThrowAnyException();
        assertThatCode(() -> service.enviarSenhaInicialAdmin("equipe@escola.edu.br", "Bibliotecário", "x"))
                .doesNotThrowAnyException();
        assertThatCode(() -> service.enviarEmailResetSenha("ada@escola.edu.br", LINK))
                .doesNotThrowAnyException();

        verify(mailSender, org.mockito.Mockito.times(3)).send(any(MimeMessage.class));
    }

    // ---- helpers -------------------------------------------------------------

    private MimeMessage enviarECapturar() {
        service.enviarSenhaInicial("ada@escola.edu.br", "Ada Lovelace", "Lm7$kP2aZ9", PT);
        return capturada();
    }

    private static String serializar(MimeMessage mensagem) throws Exception {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        mensagem.writeTo(out);
        return out.toString(java.nio.charset.StandardCharsets.UTF_8);
    }

    /** A mensagem que foi de fato entregue ao {@link JavaMailSender}. */
    private MimeMessage capturada() {
        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());
        return captor.getValue();
    }
}
