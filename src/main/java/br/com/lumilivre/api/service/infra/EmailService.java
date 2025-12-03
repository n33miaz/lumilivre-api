package br.com.lumilivre.api.service.infra;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    // private static final String LOGO_URL = "https://ylwmaozotaddmyhosiqc.supabase.co/storage/v1/object/public/capas/capas/logo-foreground%20(3).png";

    public void enviarSenhaInicial(String destino, String nome, String senha) {
        SimpleMailMessage mensagem = new SimpleMailMessage();
        mensagem.setTo(destino);
        mensagem.setSubject("Acesso ao Portal LumiLivre");
        mensagem.setText("Olá " + nome + ",\n\nSeu acesso foi criado com sucesso.\n" +
                "Login: sua matrícula\nSenha inicial: " + senha + "\n\n" +
                "Recomendamos que altere sua senha ao fazer o primeiro login.\n\nAtenciosamente,\nEquipe Lumilivre");
        mailSender.send(mensagem);
    }

    public void enviarSenhaInicialAdmin(String destino, String tipoUsuario, String senha) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");

            helper.setTo(destino);
            helper.setSubject("Bem-vindo à Equipe LumiLivre");
            helper.setFrom("contato.lumlivre@gmail.com.br");

            String htmlMsg = "<html>"
                    + "<body style='font-family: Arial, sans-serif; color: #333;'>"
                    + "<div style='padding: 20px; border: 1px solid #ddd; border-radius: 8px; max-width: 600px; margin: 0 auto;'>"
                    + "  <h2 style='color: #762075; text-align: center;'>Bem-vindo ao LumiLivre!</h2>"
                    + "  <p>Olá, Seu acesso de <strong>" + tipoUsuario + "</strong> acaba de ser criado!</p>"
                    + "  <p>Por favor, acesse o sistema e altere sua senha temporária imediatamente.</p>"
                    + "  <br/>"
                    + "  <div style='background-color: #f9f9f9; padding: 15px; border-radius: 5px;'>"
                    + "    <p style='margin: 5px 0;'><strong>Link:</strong> <a href='https://www.lumilivre.com.br' style='color: #762075;'>www.lumilivre.com.br</a></p>"
                    + "    <p style='margin: 5px 0;'><strong>Login:</strong> " + destino + "</p>"
                    + "    <p style='margin: 5px 0;'><strong>Senha Inicial:</strong> " + senha + "</p>"
                    + "  </div>"
                    + "  <br/>"
                    + "  <p style='font-size: 12px; color: #777; text-align: center;'>Atenciosamente,<br/>Equipe LumiLivre</p>"
                    + "</div>"
                    + "</body>"
                    + "</html>";

            helper.setText(htmlMsg, true);
            mailSender.send(mimeMessage);

        } catch (MessagingException e) {
            System.err.println("Erro ao enviar e-mail HTML: " + e.getMessage());
        }
    }

    public void enviarEmail(String destino, String assunto, String mensagem) {
        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setTo(destino);
        mailMessage.setSubject(assunto);
        mailMessage.setText(mensagem);
        mailMessage.setFrom("biblioteca@lumilivre.com");
        mailSender.send(mailMessage);
    }

    public void enviarEmailResetSenha(String destino, String link) {
        SimpleMailMessage mensagem = new SimpleMailMessage();
        mensagem.setTo(destino);
        mensagem.setSubject("Redefinição de Senha - LumiLivre");
        mensagem.setText("Olá,\n\nsegue a solicitação de redefinição de senha. " +
                "Clique no link a seguir para criar uma nova senha:\n" + link +
                "\n\nSe você não solicitou esta alteração, por favor ignore este e-mail.\n\n" +
                "Atenciosamente,\nEquipe LumiLivre");
        mailSender.send(mensagem);
    }
}