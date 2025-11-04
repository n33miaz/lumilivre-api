package br.com.lumilivre.api.data;

public class TccRequestDTO {
    private String titulo;
    private String alunos;
    private String orientadores;
    private String curso;
    private String anoConclusao;
    private String semestreConclusao;
    private String linkExterno;
    private Boolean ativo = true;

    // Getters e Setters
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getAlunos() { return alunos; }
    public void setAlunos(String alunos) { this.alunos = alunos; }

    public String getOrientadores() { return orientadores; }
    public void setOrientadores(String orientadores) { this.orientadores = orientadores; }

    public String getCurso() { return curso; }
    public void setCurso(String curso) { this.curso = curso; }

    public String getAnoConclusao() { return anoConclusao; }
    public void setAnoConclusao(String anoConclusao) { this.anoConclusao = anoConclusao; }

    public String getSemestreConclusao() { return semestreConclusao; }
    public void setSemestreConclusao(String semestreConclusao) { this.semestreConclusao = semestreConclusao; }

    public String getLinkExterno() { return linkExterno; }
    public void setLinkExterno(String linkExterno) { this.linkExterno = linkExterno; }

    public Boolean getAtivo() { return ativo; }
    public void setAtivo(Boolean ativo) { this.ativo = ativo; }
}
