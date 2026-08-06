package br.com.lumilivre.api.service.infra.email;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Year;
import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.context.support.StaticMessageSource;

import br.com.lumilivre.api.config.EmailBrandingProperties;
import br.com.lumilivre.api.config.MessageResolver;

/**
 * O casco visual compartilhado por todo e-mail transacional. Duas coisas aqui
 * são regra e não estética: <b>escapar</b> o que veio do usuário (o corpo do
 * e-mail é HTML, e nome de leitor é texto de terceiro) e a origem do logotipo,
 * que decide se a mensagem carrega a imagem embutida ou depende de host
 * externo.
 */
class EmailTemplateTest {

    private static final Locale PT = Locale.forLanguageTag("pt-BR");
    private static final Locale EN = Locale.forLanguageTag("en-US");

    private final EmailBrandingProperties branding = new EmailBrandingProperties();
    private final EmailTemplate template = new EmailTemplate(branding, new MessageResolver(bundle()));

    // ---- escape --------------------------------------------------------------

    /**
     * O nome do leitor vem de um cadastro que a secretaria digita. Se ele
     * entrasse cru, um {@code <script>} no campo "nome completo" viraria markup
     * na caixa de entrada de quem recebe — e o mesmo vale para o assunto
     * genérico do outbox, que carrega título de livro.
     */
    @Test
    void oQueVeioDoUsuarioNuncaViraMarkup() {
        String escapado = EmailTemplate.escape("<script>alert(\"x\")</script> & cia");

        assertThat(escapado)
                .doesNotContain("<script>", "</script>")
                .isEqualTo("&lt;script&gt;alert(&quot;x&quot;)&lt;/script&gt; &amp; cia");
    }

    @Test
    void oAmpersandEEscapadoAntesDosOutrosParaNaoDuplicar() {
        // Se "&" fosse trocado depois de "<", o resultado seria "&amp;lt;".
        assertThat(EmailTemplate.escape("<")).isEqualTo("&lt;");
        assertThat(EmailTemplate.escape("&lt;")).isEqualTo("&amp;lt;");
    }

    @Test
    void textoAusenteViraStringVaziaENaoANulaLiteral() {
        assertThat(EmailTemplate.escape(null)).isEmpty();
    }

    // ---- logotipo ------------------------------------------------------------

    /**
     * Sem URL configurada o logotipo vai embutido (CID). É o padrão de propósito:
     * imagem hospedada fora chega bloqueada por padrão no Gmail e no Outlook, e o
     * cabeçalho do e-mail chegaria vazio em boa parte das caixas.
     */
    @Test
    void semUrlConfiguradaOLogotipoVaiEmbutido() {
        assertThat(template.usesInlineLogo()).isTrue();
        assertThat(template.logoSrc()).isEqualTo("cid:" + EmailTemplate.LOGO_CID);
    }

    @Test
    void comUrlConfiguradaOLogotipoPassaAApontarParaFora() {
        branding.setLogoUrl("https://cdn.lumilivre.com.br/logo.png");

        assertThat(template.usesInlineLogo()).isFalse();
        assertThat(template.logoSrc()).isEqualTo("https://cdn.lumilivre.com.br/logo.png");
    }

    // ---- casco ---------------------------------------------------------------

    @Test
    void oCascoCarregaOIdiomaAMarcaEOAnoCorrente() {
        String html = template.render("<p>miolo</p>", "prévia", PT);

        assertThat(html)
                .startsWith("<!DOCTYPE html>")
                .contains("lang=\"pt-BR\"")
                .contains("LumiLivre")
                .contains("<p>miolo</p>")
                .contains("&copy; " + Year.now().getValue())
                .contains("Esta é uma mensagem automática")
                .endsWith("</body></html>");
    }

    @Test
    void oIdiomaDoDestinatarioTrocaOTextoDoCasco() {
        assertThat(template.render("", "preview", EN))
                .contains("lang=\"en-US\"")
                .contains("This is an automated message")
                .contains("All rights reserved.");
    }

    /**
     * A prévia da caixa de entrada é a primeira coisa que o destinatário lê, e
     * ela também é texto vindo de dado (título de livro, nome). Escapar ali é
     * tão obrigatório quanto no corpo.
     */
    @Test
    void aPreviaDaCaixaDeEntradaTambemEEscapada() {
        assertThat(template.render("", "<b>Dom & Casmurro</b>", PT))
                .contains("&lt;b&gt;Dom &amp; Casmurro&lt;/b&gt;")
                .doesNotContain("<b>Dom");
    }

    /**
     * Endereço e e-mail de suporte são opcionais por implantação. Em branco, a
     * linha inteira some — nada de rodapé com "Precisa de ajuda? Fale com a
     * gente em ." que é o que sairia se só o valor fosse omitido.
     */
    @Test
    void oRodapeSoMostraSuporteEEnderecoQuandoConfigurados() {
        String semExtras = template.render("", "prévia", PT);
        assertThat(semExtras)
                .doesNotContain("Precisa de ajuda?")
                .doesNotContain("Av. das Bibliotecas");

        branding.setSupportEmail("suporte@lumilivre.com.br");
        branding.setAddressLine("Av. das Bibliotecas, 1000");
        String comExtras = template.render("", "prévia", PT);
        assertThat(comExtras)
                .contains("Precisa de ajuda? Fale com a gente em suporte@lumilivre.com.br.")
                .contains("Av. das Bibliotecas, 1000");
    }

    /**
     * Sem a chave no bundle, o casco cai para o tagline da configuração de marca
     * — nunca para o nome cru da chave, que é o que o {@code MessageResolver}
     * devolve por padrão e apareceria como {@code email.shell.tagline} no
     * cabeçalho.
     */
    @Test
    void semTraducaoDoTaglineOCascoUsaOTaglineDaMarca() {
        branding.setTagline("Biblioteca da Escola Modelo");
        EmailTemplate semBundle = new EmailTemplate(branding, new MessageResolver(new StaticMessageSource()));

        assertThat(semBundle.render("", "prévia", PT))
                .contains("Biblioteca da Escola Modelo")
                .doesNotContain("email.shell.tagline");
    }

    // ---- componentes ---------------------------------------------------------

    @Test
    void oCartaoDeCredenciaisEmparelhaRotuloEValor() {
        String html = template.infoCard("Login:", "ada@escola.edu.br", "Senha Inicial:", template.code("Lm7$kP2aZ9"));

        assertThat(html)
                .contains("Login:")
                .contains("ada@escola.edu.br")
                .contains("Senha Inicial:")
                .contains("Lm7$kP2aZ9");
    }

    /** Rótulo é escapado; valor não, porque o chamador pode mandar markup pronto. */
    @Test
    void oRotuloDoCartaoEEscapadoEOValorPassaComoVeio() {
        String html = template.infoCard("<b>Rótulo</b>", "<span>valor</span>");

        assertThat(html)
                .contains("&lt;b&gt;Rótulo&lt;/b&gt;")
                .contains("<span>valor</span>");
    }

    /** Par ímpar não quebra o cartão: o valor sem rótulo é simplesmente ignorado. */
    @Test
    void umParIncompletoNaoQuebraOCartao() {
        assertThat(template.infoCard("Login:")).doesNotContain("Login:");
    }

    @Test
    void oCodigoDestacadoEEscapado() {
        assertThat(template.code("<senha>")).contains("&lt;senha&gt;").doesNotContain("<senha>");
    }

    /**
     * O href do botão sai como veio (é URL montada pelo servidor, não digitada
     * pelo usuário), mas o rótulo visível é escapado.
     */
    @Test
    void oBotaoLevaAUrlNoHrefEEscapaORotulo() {
        String html = template.button("Redefinir & entrar", "https://lumilivre.com.br/reset?token=abc123");

        assertThat(html)
                .contains("href=\"https://lumilivre.com.br/reset?token=abc123\"")
                .contains("Redefinir &amp; entrar");
    }

    @Test
    void tituloParagrafoEAvisoAceitamMarkupJaMontado() {
        assertThat(template.heading("Acesso criado")).contains("<h1").contains("Acesso criado");
        assertThat(template.paragraph("Olá <strong>Ada</strong>,")).contains("Olá <strong>Ada</strong>,");
        assertThat(template.callout("Guarde <em>bem</em>.")).contains("Guarde <em>bem</em>.");
    }

    private static ReloadableResourceBundleMessageSource bundle() {
        ReloadableResourceBundleMessageSource source = new ReloadableResourceBundleMessageSource();
        source.setBasenames("classpath:i18n/email/messages");
        source.setDefaultEncoding("UTF-8");
        source.setFallbackToSystemLocale(false);
        return source;
    }
}
