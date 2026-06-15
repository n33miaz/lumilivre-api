package br.com.lumilivre.api.service.infra;

import java.util.Locale;

import br.com.lumilivre.api.config.EmailBrandingProperties;
import br.com.lumilivre.api.config.MessageResolver;
import br.com.lumilivre.api.service.infra.email.EmailTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;

/**
 * Builds and dispatches every transactional email. Rendering is separated from
 * transport: each {@code build*} method composes a {@link RenderedEmail} from
 * reusable, branded components wrapped in the single {@link EmailTemplate} shell,
 * and {@link #sendHtml} handles SMTP delivery. This keeps the visual identity
 * (logo + brand colors + footer) consistent and customizable in one place, makes
 * the HTML unit-testable, and lets tooling preview the exact production markup.
 * Copy is fully internationalized (ADR-009): the effective {@link Locale} drives
 * both the body and the shell.
 */
@Service
@RequiredArgsConstructor
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private static final Locale DEFAULT_LOCALE = Locale.forLanguageTag("pt-BR");
    private static final String LOGO_RESOURCE = "email/logo-light.png";

    private final JavaMailSender mailSender;
    private final MessageResolver messages;
    private final EmailTemplate template;
    private final EmailBrandingProperties branding;

    @Value("${app.email.from:contato.lumilivre@gmail.com}")
    private String from;

    @Value("${app.public-url:https://www.lumilivre.com.br}")
    private String site;

    /** A fully-rendered email ready for transport (or preview). */
    public record RenderedEmail(String subject, String preheader, String html) {
    }

    // ===================== INITIAL PASSWORD (USER) =====================

    public void enviarSenhaInicial(String destino, String nome, String senha) {
        enviarSenhaInicial(destino, nome, senha, LocaleContextHolder.getLocale());
    }

    public void enviarSenhaInicial(String destino, String nome, String senha, Locale locale) {
        sendHtml(destino, buildSenhaInicial(destino, nome, senha, resolveLocale(locale)));
    }

    RenderedEmail buildSenhaInicial(String destino, String nome, String senha, Locale loc) {
        String subject = messages.resolve("email.initial-password.user.subject", loc);
        String greeting = messages.resolve("email.initial-password.user.greeting", loc, EmailTemplate.escape(nome));
        String body = messages.resolve("email.initial-password.user.body", loc);
        String loginLabel = messages.resolve("email.initial-password.user.login-label", loc);
        String passwordLabel = messages.resolve("email.initial-password.user.password-label", loc);
        String advice = messages.resolve("email.initial-password.user.advice", loc);
        String cta = messages.resolve("email.initial-password.user.cta", loc);

        String inner = template.heading(subject)
                + template.paragraph(greeting)
                + template.paragraph(body)
                + template.infoCard(loginLabel, EmailTemplate.escape(destino), passwordLabel, template.code(senha))
                + template.callout(advice)
                + template.button(cta, site);

        return new RenderedEmail(subject, body, template.render(inner, body, loc));
    }

    // ===================== INITIAL PASSWORD (ADMIN/STAFF) =====================

    public void enviarSenhaInicialAdmin(String destino, String tipoUsuario, String senha) {
        enviarSenhaInicialAdmin(destino, tipoUsuario, senha, LocaleContextHolder.getLocale());
    }

    public void enviarSenhaInicialAdmin(String destino, String tipoUsuario, String senha, Locale locale) {
        sendHtml(destino, buildSenhaInicialAdmin(destino, tipoUsuario, senha, resolveLocale(locale)));
    }

    RenderedEmail buildSenhaInicialAdmin(String destino, String tipoUsuario, String senha, Locale loc) {
        String subject = messages.resolve("email.initial-password.admin.subject", loc);
        String greeting = messages.resolve("email.initial-password.admin.greeting", loc);
        String body = messages.resolve("email.initial-password.admin.body", loc, EmailTemplate.escape(tipoUsuario));
        String advice = messages.resolve("email.initial-password.admin.advice", loc);
        String loginLabel = messages.resolve("email.initial-password.admin.login-label", loc);
        String passwordLabel = messages.resolve("email.initial-password.admin.password-label", loc);
        String cta = messages.resolve("email.initial-password.admin.cta", loc);

        String inner = template.heading(subject)
                + template.paragraph(greeting)
                + template.paragraph(body)
                + template.infoCard(loginLabel, EmailTemplate.escape(destino), passwordLabel, template.code(senha))
                + template.callout(advice)
                + template.button(cta, site);

        return new RenderedEmail(subject, stripTags(body), template.render(inner, stripTags(body), loc));
    }

    // ===================== GENERIC (OUTBOX) =====================

    public void enviarEmail(String destino, String assunto, String mensagem) {
        enviarEmail(destino, assunto, mensagem, LocaleContextHolder.getLocale());
    }

    public void enviarEmail(String destino, String assunto, String mensagem, Locale locale) {
        sendHtml(destino, buildGenerico(assunto, mensagem, resolveLocale(locale)));
    }

    RenderedEmail buildGenerico(String assunto, String mensagem, Locale loc) {
        String cta = messages.resolve("email.shell.cta.portal", loc);
        String inner = template.heading(assunto)
                + template.paragraph(EmailTemplate.escape(mensagem).replace("\n", "<br/>"))
                + template.button(cta, site);
        return new RenderedEmail(assunto, mensagem, template.render(inner, mensagem, loc));
    }

    // ===================== PASSWORD RESET =====================

    public void enviarEmailResetSenha(String destino, String link) {
        enviarEmailResetSenha(destino, link, DEFAULT_LOCALE);
    }

    public void enviarEmailResetSenha(String destino, String link, Locale locale) {
        sendHtml(destino, buildResetSenha(link, resolveLocale(locale)));
    }

    RenderedEmail buildResetSenha(String link, Locale loc) {
        String subject = messages.resolve("email.reset-password.subject", loc);
        String greeting = messages.resolve("email.reset-password.greeting", loc);
        String body = messages.resolve("email.reset-password.body", loc);
        String ignore = messages.resolve("email.reset-password.ignore", loc);
        String cta = messages.resolve("email.reset-password.cta", loc);
        String fallback = messages.resolve("email.reset-password.fallback", loc);

        String inner = template.heading(subject)
                + template.paragraph(greeting)
                + template.paragraph(body)
                + template.button(cta, link)
                + template.callout(ignore)
                + template.paragraph("<span style=\"font-size:13px;color:" + branding.getMutedColor() + ";\">"
                        + fallback + "<br/><a href=\"" + link + "\" style=\"color:" + branding.getPrimaryColor()
                        + ";word-break:break-all;\">" + EmailTemplate.escape(link) + "</a></span>");

        return new RenderedEmail(subject, body, template.render(inner, body, loc));
    }

    // ===================== LOAN DUE-DATE NOTIFICATIONS =====================

    public void enviarNotificacaoEmprestimo(String destino, String subjectKey, String bodyKey,
            String bookTitle, Locale locale) {
        sendHtml(destino, buildNotificacaoEmprestimo(subjectKey, bodyKey, bookTitle, resolveLocale(locale)));
    }

    RenderedEmail buildNotificacaoEmprestimo(String subjectKey, String bodyKey, String bookTitle, Locale loc) {
        String subject = messages.resolve(subjectKey, loc);
        String bodyText = messages.resolve(bodyKey, loc, bookTitle);
        String cta = messages.resolve("email.shell.cta.portal", loc);

        String inner = template.heading(subject)
                + template.paragraph(EmailTemplate.escape(bodyText))
                + template.button(cta, site);

        return new RenderedEmail(subject, bodyText, template.render(inner, bodyText, loc));
    }

    // ============================ TRANSPORT ============================

    /**
     * Sends a pre-rendered email. The bundled logo is embedded inline (CID)
     * unless an external logo URL is configured, so the header renders without
     * external image hosting. SMTP failures are logged, not thrown, preserving
     * the outbox retry semantics.
     */
    private void sendHtml(String destino, RenderedEmail email) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            boolean inlineLogo = template.usesInlineLogo();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, inlineLogo, "utf-8");

            helper.setTo(destino);
            helper.setSubject(email.subject());
            helper.setFrom(from);
            helper.setText(email.html(), true);

            if (inlineLogo) {
                helper.addInline(EmailTemplate.LOGO_CID, new ClassPathResource(LOGO_RESOURCE), "image/png");
            }

            mailSender.send(mimeMessage);
        } catch (MessagingException e) {
            log.error(messages.resolve("email.send.error", DEFAULT_LOCALE, e.getMessage()), e);
        }
    }

    // ============================ HELPERS ============================

    private Locale resolveLocale(Locale locale) {
        return locale != null ? locale : DEFAULT_LOCALE;
    }

    private static String stripTags(String s) {
        return s == null ? "" : s.replaceAll("<[^>]+>", "");
    }
}
