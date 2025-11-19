package br.com.lumilivre.api.dto.comum;

public class EnumResponse {
    private String nome;
    private String status;

    public EnumResponse(String nome, String status) {
        this.nome = nome;
        this.status = status;
    }

    public String getNome() {
        return nome;
    }

    public String getStatus() {
        return status;
    }
}
