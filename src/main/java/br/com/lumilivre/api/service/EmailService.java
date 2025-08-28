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
}
