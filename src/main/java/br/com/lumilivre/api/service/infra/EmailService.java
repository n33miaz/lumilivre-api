package br.com.lumilivre.api.service.infra;

import java.util.Locale;

import br.com.lumilivre.api.config.MessageResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private static final Locale DEFAULT_LOCALE = Locale.forLanguageTag("pt-BR");

    private final JavaMailSender mailSender;
    private final MessageResolver messages;

    @Value("${app.email.from:contato.lumilivre@gmail.com}")
    private String from;

    @Value("${app.public-url:https://www.lumilivre.com.br}")
    private String site;

    private void enviarEmailHtml(String destino, String assunto, String conteudoHtml, Locale locale) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");

            helper.setTo(destino);
            helper.setSubject(assunto);
            helper.setFrom(from);

            String signature = messages.resolve("email.footer.signature", locale);
            String brand = messages.resolve("email.footer.brand", locale);

            String htmlMsg = "<html>" +
                    "<body style='font-family: Arial, sans-serif; color: #333;'>" +
                    "<div style='padding: 20px; border: 1px solid #ddd; border-radius: 8px; max-width: 600px; margin: 0 auto;'>"
                    +
                    "  <h2 style='color: #762075; text-align: center;'>LumiLivre</h2>" +
                    conteudoHtml +
                    "  <br/>" +
                    "  <p style='font-size: 12px; color: #777; text-align: center;'>" + signature + "<br/>" + brand
                    + "</p>" +
                    "</div>" +
                    "</body>" +
                    "</html>";

            helper.setText(htmlMsg, true);
            mailSender.send(mimeMessage);

        } catch (MessagingException e) {
            log.error(messages.resolve("email.send.error", locale, e.getMessage()), e);
        }
    }

    public void enviarSenhaInicial(String destino, String nome, String senha) {
        enviarSenhaInicial(destino, nome, senha, LocaleContextHolder.getLocale());
    }

    public void enviarSenhaInicial(String destino, String nome, String senha, Locale locale) {
        Locale effectiveLocale = resolveLocale(locale);
        String subject = messages.resolve("email.initial-password.user.subject", effectiveLocale);
        String greeting = messages.resolve("email.initial-password.user.greeting", effectiveLocale, nome);
        String body = messages.resolve("email.initial-password.user.body", effectiveLocale);
        String loginLabel = messages.resolve("email.initial-password.user.login-label", effectiveLocale);
        String passwordLabel = messages.resolve("email.initial-password.user.password-label", effectiveLocale);
        String advice = messages.resolve("email.initial-password.user.advice", effectiveLocale);
        String linkLabel = messages.resolve("email.initial-password.user.link-label", effectiveLocale);

        String conteudo = "<p>" + greeting + "</p>" +
                "<p>" + body + "</p>" +
                "<div style='background-color: #f9f9f9; padding: 15px; border-radius: 5px;'>" +
                "  <p><strong>" + loginLabel + "</strong> " + senha + "</p>" +
                "  <p><strong>" + passwordLabel + "</strong> " + senha + "</p>" +
                "</div>" +
                "<p>" + advice + "</p>" +
                "<p><strong>" + linkLabel + "</strong> <a href='" + site + "'>" + site + "</a></p>";

        enviarEmailHtml(destino, subject, conteudo, effectiveLocale);
    }

    public void enviarSenhaInicialAdmin(String destino, String tipoUsuario, String senha) {
        enviarSenhaInicialAdmin(destino, tipoUsuario, senha, LocaleContextHolder.getLocale());
    }

    public void enviarSenhaInicialAdmin(String destino, String tipoUsuario, String senha, Locale locale) {
        Locale effectiveLocale = resolveLocale(locale);
        String subject = messages.resolve("email.initial-password.admin.subject", effectiveLocale);
        String greeting = messages.resolve("email.initial-password.admin.greeting", effectiveLocale);
        String body = messages.resolve("email.initial-password.admin.body", effectiveLocale, tipoUsuario);
        String advice = messages.resolve("email.initial-password.admin.advice", effectiveLocale);
        String loginLabel = messages.resolve("email.initial-password.admin.login-label", effectiveLocale);
        String passwordLabel = messages.resolve("email.initial-password.admin.password-label", effectiveLocale);
        String linkLabel = messages.resolve("email.initial-password.admin.link-label", effectiveLocale);

        String conteudo = "<p>" + greeting + "</p>" +
                "<p>" + body + "</p>" +
                "<p>" + advice + "</p>" +
                "<div style='background-color: #f9f9f9; padding: 15px; border-radius: 5px;'>" +
                "  <p><strong>" + loginLabel + "</strong> " + destino + "</p>" +
                "  <p><strong>" + passwordLabel + "</strong> " + senha + "</p>" +
                "  <p><strong>" + linkLabel + "</strong> <a href='" + site + "'>" + site + "</a></p>" +
                "</div>";

        enviarEmailHtml(destino, subject, conteudo, effectiveLocale);
    }

    public void enviarEmail(String destino, String assunto, String mensagem) {
        enviarEmail(destino, assunto, mensagem, LocaleContextHolder.getLocale());
    }

    public void enviarEmail(String destino, String assunto, String mensagem, Locale locale) {
        String conteudo = "<p>" + mensagem.replace("\n", "<br/>") + "</p>";
        enviarEmailHtml(destino, assunto, conteudo, resolveLocale(locale));
    }

    public void enviarEmailResetSenha(String destino, String link) {
        enviarEmailResetSenha(destino, link, DEFAULT_LOCALE);
    }

    public void enviarEmailResetSenha(String destino, String link, Locale locale) {
        Locale effectiveLocale = resolveLocale(locale);
        String subject = messages.resolve("email.reset-password.subject", effectiveLocale);
        String greeting = messages.resolve("email.reset-password.greeting", effectiveLocale);
        String body = messages.resolve("email.reset-password.body", effectiveLocale);
        String ignore = messages.resolve("email.reset-password.ignore", effectiveLocale);

        String conteudo = "<p>" + greeting + "</p>" +
                "<p>" + body + "</p>" +
                "<p style='text-align:center;'>" +
                "  <a href='" + link
                + "' style='background:#762075; color:white; padding:10px 20px; text-decoration:none; border-radius:5px;'>"
                + subject + "</a>" +
                "</p>" +
                "<p>" + ignore + "</p>";

        enviarEmailHtml(destino, subject, conteudo, effectiveLocale);
    }

    public void enviarNotificacaoEmprestimo(String destino, String subjectKey, String bodyKey,
            String bookTitle, Locale locale) {
        Locale effectiveLocale = resolveLocale(locale);
        String subject = messages.resolve(subjectKey, effectiveLocale);
        String bodyText = messages.resolve(bodyKey, effectiveLocale, bookTitle);
        String conteudo = "<p>" + bodyText + "</p>";
        enviarEmailHtml(destino, subject, conteudo, effectiveLocale);
    }

    private Locale resolveLocale(Locale locale) {
        return locale != null ? locale : DEFAULT_LOCALE;
    }
}
