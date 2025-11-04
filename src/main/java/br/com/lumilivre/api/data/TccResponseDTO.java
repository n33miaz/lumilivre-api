package br.com.lumilivre.api.data;

public class TccResponseDTO {
    private Long id;
    private String titulo;
    private String alunos;
    private String orientadores;
    private String curso;
    private String anoConclusao;
    private String semestreConclusao;
    private String arquivoPdf;
    private String linkExterno;
    private Boolean ativo;

    // Construtor rápido (facilita conversão)
    public TccResponseDTO(Long id, String titulo, String alunos, String orientadores,
                          String curso, String anoConclusao, String semestreConclusao,
                          String arquivoPdf, String linkExterno, Boolean ativo) {
        this.id = id;
        this.titulo = titulo;
        this.alunos = alunos;
        this.orientadores = orientadores;
        this.curso = curso;
        this.anoConclusao = anoConclusao;
        this.semestreConclusao = semestreConclusao;
        this.arquivoPdf = arquivoPdf;
        this.linkExterno = linkExterno;
        this.ativo = ativo;
    }

    // Getters
    public Long getId() { return id; }
    public String getTitulo() { return titulo; }
    public String getAlunos() { return alunos; }
    public String getOrientadores() { return orientadores; }
    public String getCurso() { return curso; }
    public String getAnoConclusao() { return anoConclusao; }
    public String getSemestreConclusao() { return semestreConclusao; }
    public String getArquivoPdf() { return arquivoPdf; }
    public String getLinkExterno() { return linkExterno; }
    public Boolean getAtivo() { return ativo; }
}
