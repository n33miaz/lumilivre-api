package br.com.lumilivre.api.model;

import java.time.LocalDateTime;

import br.com.lumilivre.api.enums.StatusReserva;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Fila de reserva (FIFO por livro).
 * Uma reserva representa a intenção de empréstimo quando não há exemplar disponível.
 */
@Entity
@Table(name = "reserva")
@Getter
@Setter
@NoArgsConstructor
public class ReservaModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "aluno_id", nullable = false)
    private AlunoModel aluno;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "livro_id", nullable = false)
    private LivroModel livro;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private StatusReserva status = StatusReserva.WAITING;

    /** Posição FIFO na fila para este livro (1 = próximo) */
    @Column(nullable = false)
    private Integer posicaoFila;

    @Column(nullable = false)
    private LocalDateTime criadaEm = LocalDateTime.now();

    /** Prazo máximo para retirada após notificação DISPONIVEL_PARA_RETIRADA */
    private LocalDateTime expiraEm;

    /** Quando o aluno foi notificado da disponibilidade */
    private LocalDateTime notificadoEm;
}
