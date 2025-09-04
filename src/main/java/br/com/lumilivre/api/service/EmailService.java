package br.com.lumilivre.api.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    public void enviarSenhaInicial(String destino, String nome, String senha) {
        SimpleMailMessage mensagem = new SimpleMailMessage();
        mensagem.setTo(destino);
        mensagem.setSubject("Acesso ao Portal Lumilivre");
        mensagem.setText("Olá " + nome + ",\n\nSeu acesso foi criado com sucesso.\n" +
                "Login: sua matrícula\nSenha inicial: " + senha + "\n\n" +
                "Recomendamos que altere sua senha ao fazer o primeiro login.\n\nAtenciosamente,\nEquipe Lumilivre");
        mailSender.send(mensagem);
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
        mensagem.setSubject("Redefinição de Senha - Lumi Livre");
        mensagem.setText("Olá,\n\nVocê solicitou a redefinição de sua senha. " +
                "Clique no link a seguir para criar uma nova senha:\n" + link +
                "\n\nSe você não solicitou esta alteração, por favor ignore este e-mail.\n\n" +
                "Atenciosamente,\nEquipe Lumi Livre");
        mailSender.send(mensagem);
}
}