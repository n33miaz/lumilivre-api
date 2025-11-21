package br.com.lumilivre.api.dto.tcc;

import br.com.lumilivre.api.model.TccModel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TccResponse {

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

    public TccResponse(TccModel tcc) {
        this.id = tcc.getId();
        this.titulo = tcc.getTitulo();
        this.alunos = tcc.getAlunos();
        this.orientadores = tcc.getOrientadores();
        this.curso = tcc.getCurso() != null ? tcc.getCurso().getNome() : null;
        this.anoConclusao = tcc.getAnoConclusao();
        this.semestreConclusao = tcc.getSemestreConclusao();
        this.arquivoPdf = tcc.getArquivoPdf();
        this.linkExterno = tcc.getLinkExterno();
        this.ativo = tcc.getAtivo();
    }
}