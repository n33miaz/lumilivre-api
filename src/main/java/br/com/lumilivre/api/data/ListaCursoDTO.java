package br.com.lumilivre.api.data;

import br.com.lumilivre.api.enums.Turno;

public class ListaCursoDTO {
    private String nome;
    private Turno turno;
    private String modulo;

    public ListaCursoDTO(String nome, Turno turno, String modulo) {
        this.nome = nome;
        this.turno = turno;
        this.modulo = modulo;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public Turno getTurno() {
        return turno;
    }

    public void setTurno(Turno turno) {
        this.turno = turno;
    }

    public String getModulo() {
        return modulo;
    }

    public void setModulo(String modulo) {
        this.modulo = modulo;
    }

}
