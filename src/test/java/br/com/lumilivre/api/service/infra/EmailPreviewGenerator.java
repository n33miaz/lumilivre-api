package br.com.lumilivre.api.service.infra;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.test.util.ReflectionTestUtils;

import br.com.lumilivre.api.config.EmailBrandingProperties;
import br.com.lumilivre.api.config.MessageResolver;
import br.com.lumilivre.api.service.infra.EmailService.RenderedEmail;
import br.com.lumilivre.api.service.infra.email.EmailTemplate;

/**
 * Manual preview harness (NOT part of the CI suite — its name does not match the
 * Surefire include patterns). Renders the exact production email markup for a set
 * of representative scenarios in both locales and writes them to
 * {@code target/email-previews/}. Run on demand:
 *
 * <pre>./mvnw -Dtest=EmailPreviewGenerator test</pre>
 */
class EmailPreviewGenerator {

    private static final Locale PT = Locale.forLanguageTag("pt-BR");
    private static final Locale EN = Locale.forLanguageTag("en-US");

    @Test
    void generate() throws Exception {
        EmailBrandingProperties branding = new EmailBrandingProperties();
        branding.setSupportEmail("suporte@lumilivre.com.br");
        branding.setAddressLine("Av. das Bibliotecas, 1000 · São Paulo · Brasil");

        ReloadableResourceBundleMessageSource source = new ReloadableResourceBundleMessageSource();
        source.setBasenames("classpath:i18n/email/messages");
        source.setDefaultEncoding("UTF-8");
        source.setFallbackToSystemLocale(false);

        MessageResolver messages = new MessageResolver(source);
        EmailTemplate template = new EmailTemplate(branding, messages);
        EmailService service = new EmailService(null, messages, template, branding);
        ReflectionTestUtils.setField(service, "from", "contato.lumilivre@gmail.com");
        ReflectionTestUtils.setField(service, "site", "https://www.lumilivre.com.br");

        Path dir = Path.of("target", "email-previews");
        Files.createDirectories(dir);

        write(dir, "01-initial-password-user-pt",
                service.buildSenhaInicial("ada.lovelace@escola.edu.br", "Ada Lovelace", "Lm7$kP2aZ9", PT));
        write(dir, "02-initial-password-user-en",
                service.buildSenhaInicial("ada.lovelace@escola.edu.br", "Ada Lovelace", "Lm7$kP2aZ9", EN));
        write(dir, "03-initial-password-admin-pt",
                service.buildSenhaInicialAdmin("bibliotecario@escola.edu.br",
                        messages.resolve("email.initial-password.admin.body", PT), "Adm!n9xQ2w", PT));
        write(dir, "04-reset-password-pt",
                service.buildResetSenha("https://www.lumilivre.com.br/reset?token=8f3a2c9e-7b21-4d6f-9a0c-1e2b3c4d5e6f", PT));
        write(dir, "05-reset-password-en",
                service.buildResetSenha("https://www.lumilivre.com.br/reset?token=8f3a2c9e-7b21-4d6f-9a0c-1e2b3c4d5e6f", EN));
        write(dir, "06-loan-created-pt",
                service.buildGenerico(
                        messages.resolve("email.loan-created.subject", PT),
                        messages.resolve("email.loan-created.body", PT,
                                "Ada Lovelace", "Clean Code", "29/05/2026", "12/06/2026"),
                        PT));
        write(dir, "07-loan-overdue-en",
                service.buildNotificacaoEmprestimo("email.loan-overdue.subject", "email.loan-overdue.body",
                        "Clean Code", EN));
        write(dir, "08-reservation-ready-pt",
                service.buildGenerico(
                        messages.resolve("email.reservation-ready.subject", PT),
                        messages.resolve("email.reservation-ready.body", PT, "Clean Code", "31/05/2026"),
                        PT));

        System.out.println("[email-previews] written to: " + dir.toAbsolutePath());
    }

    private static void write(Path dir, String name, RenderedEmail email) throws Exception {
        Files.writeString(dir.resolve(name + ".html"), email.html(), StandardCharsets.UTF_8);
    }
}
