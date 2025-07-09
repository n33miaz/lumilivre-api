package br.com.lumilivre.api.model;

import java.time.LocalDateTime;

import br.com.lumilivre.api.enums.Penalidade;
import br.com.lumilivre.api.enums.StatusEmprestimo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "emprestimo")
public class EmprestimoModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "data_emprestimo", nullable = false)
    private LocalDateTime data_emprestimo;

    @NotNull
    @Column(name = "data_devolucao", nullable = false)
    private LocalDateTime data_devolucao;

    @Enumerated(EnumType.STRING)
    @Column(name = "penalidade", length = 55)
    private Penalidade penalidade;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_emprestimo", length = 55)
    private StatusEmprestimo status_emprestimo;

    @ManyToOne
    @JoinColumn(name = "aluno_matricula", nullable = false)
    private AlunoModel aluno;

    @ManyToOne
    @JoinColumn(name = "exemplar_tombo", nullable = false) 
    private ExemplarModel exemplar;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getData_emprestimo() {
        return data_emprestimo;
    }

    public void setData_emprestimo(LocalDateTime data_emprestimo) {
        this.data_emprestimo = data_emprestimo;
    }

    public LocalDateTime getData_devolucao() {
        return data_devolucao;
    }

    public void setData_devolucao(LocalDateTime data_devolucao) {
        this.data_devolucao = data_devolucao;
    }

    public Penalidade getPenalidade() {
        return penalidade;
    }

    public void setPenalidade(Penalidade penalidade) {
        this.penalidade = penalidade;
    }

    public StatusEmprestimo getStatus_emprestimo() {
        return status_emprestimo;
    }

    public void setStatus_emprestimo(StatusEmprestimo status_emprestimo) {
        this.status_emprestimo = status_emprestimo;
    }

    public AlunoModel getAluno() {
        return aluno;
    }

    public void setAluno(AlunoModel aluno) {
        this.aluno = aluno;
    }

    public ExemplarModel getExemplar() {
        return exemplar;
    }

    public void setExemplar(ExemplarModel exemplar) {
        this.exemplar = exemplar;
    }


}
