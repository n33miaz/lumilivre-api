package br.com.lumilivre.api.dto;

public class EnumDTO {
    private String nome;
    private String status;

    public EnumDTO(String nome, String status) {
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
