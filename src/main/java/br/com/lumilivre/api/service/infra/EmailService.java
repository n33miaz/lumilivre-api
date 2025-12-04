package br.com.lumilivre.api.service.infra;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    private static final String FROM = "contato.lumlivre@gmail.com.br";
    private static final String SITE = "https://www.lumilivre.com.br";

    // =========================
    // MÉTODO BASE (PADRÃO HTML)
    // =========================
    private void enviarEmailHtml(String destino, String assunto, String conteudoHtml) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");

            helper.setTo(destino);
            helper.setSubject(assunto);
            helper.setFrom(FROM);

            String htmlMsg = "<html>" +
                    "<body style='font-family: Arial, sans-serif; color: #333;'>" +
                    "<div style='padding: 20px; border: 1px solid #ddd; border-radius: 8px; max-width: 600px; margin: 0 auto;'>"
                    +
                    "  <h2 style='color: #762075; text-align: center;'>LumiLivre</h2>" +
                    conteudoHtml +
                    "  <br/>" +
                    "  <p style='font-size: 12px; color: #777; text-align: center;'>Atenciosamente,<br/>Equipe LumiLivre</p>"
                    +
                    "</div>" +
                    "</body>" +
                    "</html>";

            helper.setText(htmlMsg, true);
            mailSender.send(mimeMessage);

        } catch (MessagingException e) {
            System.err.println("Erro ao enviar e-mail HTML: " + e.getMessage());
        }
    }

    // =========================
    // SENHA INICIAL - USUÁRIO
    // =========================
    public void enviarSenhaInicial(String destino, String nome, String senha) {

        String conteudo = "<p>Olá <strong>" + nome + "</strong>,</p>" +
                "<p>Seu acesso foi criado com sucesso!</p>" +
                "<div style='background-color: #f9f9f9; padding: 15px; border-radius: 5px;'>" +
                "  <p><strong>Login:</strong> " + senha + "</p>" +
                "  <p><strong>Senha Inicial:</strong> " + senha + "</p>" +
                "</div>" +
                "<p>Recomendamos que altere sua senha no primeiro acesso.</p>" +
                "<p><strong>Link:</strong> <a href='" + SITE + "'>" + SITE + "</a></p>";

        enviarEmailHtml(destino, "Acesso ao Portal LumiLivre", conteudo);
    }

    // =========================
    // SENHA INICIAL - ADMIN
    // =========================
    public void enviarSenhaInicialAdmin(String destino, String tipoUsuario, String senha) {

        String conteudo = "<p>Olá,</p>" +
                "<p>Seu acesso de <strong>" + tipoUsuario + "</strong> foi criado com sucesso.</p>" +
                "<p>Altere sua senha no primeiro acesso.</p>" +
                "<div style='background-color: #f9f9f9; padding: 15px; border-radius: 5px;'>" +
                "  <p><strong>Login:</strong> " + destino + "</p>" +
                "  <p><strong>Senha Inicial:</strong> " + senha + "</p>" +
                "  <p><strong>Link:</strong> <a href='" + SITE + "'>" + SITE + "</a></p>" +
                "</div>";

        enviarEmailHtml(destino, "Bem-vindo à Equipe LumiLivre", conteudo);
    }

    // =========================
    // E-MAIL GENÉRICO (HTML)
    // =========================
    public void enviarEmail(String destino, String assunto, String mensagem) {

        String conteudo = "<p>" + mensagem.replace("\n", "<br/>") + "</p>";

        enviarEmailHtml(destino, assunto, conteudo);
    }

    // =========================
    // RESET DE SENHA (HTML)
    // =========================
    public void enviarEmailResetSenha(String destino, String link) {

        String conteudo = "<p>Olá,</p>" +
                "<p>Recebemos uma solicitação para redefinir sua senha.</p>" +
                "<p>Clique no botão abaixo para criar uma nova senha:</p>" +
                "<p style='text-align:center;'>" +
                "  <a href='" + link
                + "' style='background:#762075; color:white; padding:10px 20px; text-decoration:none; border-radius:5px;'>"
                +
                "    Redefinir Senha" +
                "  </a>" +
                "</p>" +
                "<p>Se você não solicitou esta alteração, ignore este e-mail.</p>";

        enviarEmailHtml(destino, "Redefinição de Senha - LumiLivre", conteudo);
    }
}
