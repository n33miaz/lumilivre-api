package br.com.lumilivre.api.data;

import java.time.LocalDateTime;

public class RespostaErroDTO {

    private LocalDateTime timestamp;
    private int status;
    private String erro;
    private String mensagem;
    private String caminho;

    public RespostaErroDTO(int status, String erro, String mensagem, String caminho) {
        this.timestamp = LocalDateTime.now();
        this.status = status;
        this.erro = erro;
        this.mensagem = mensagem;
        this.caminho = caminho;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public int getStatus() {
        return status;
    }

    public String getErro() {
        return erro;
    }

    public String getMensagem() {
        return mensagem;
    }

    public String getCaminho() {
        return caminho;
    }
}