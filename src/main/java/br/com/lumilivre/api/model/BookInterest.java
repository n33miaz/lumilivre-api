package br.com.lumilivre.api.model;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Interesse de um leitor por um livro — o "curtir" do app, que antes vivia no
 * {@code SharedPreferences} do celular.
 *
 * <p>Sem {@code updated_at}: interesse não se edita, se cria e se apaga. Por
 * isso não há {@code @PreUpdate} aqui, nem trigger na V8.
 */
@Entity(name = "BookInterest")
@Table(name = "book_interest",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_book_interest_reader_book",
                columnNames = {"reader_id", "book_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookInterest {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "uuid")
    private UUID id;

    // LAZY nos dois lados: quem lista o interesse do leitor faz JOIN FETCH do
    // livro, e quem agrega o resumo nao materializa entidade nenhuma.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reader_id", nullable = false, updatable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Reader reader;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "book_id", nullable = false, updatable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Book book;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    /**
     * UTC e truncado a microssegundo — a precisão que o {@code timestamptz} do
     * Postgres guarda.
     *
     * <p>Sem isso, marcar interesse devolve um instante com nanossegundos e no
     * fuso do servidor, enquanto a releitura devolve o mesmo instante em UTC e
     * com microssegundos. São o mesmo momento, mas não o mesmo valor: a promessa
     * de "marcar duas vezes responde o mesmo corpo" passaria a valer só depois de
     * o cliente normalizar, o que é exatamente o tipo de detalhe que um cliente
     * não normaliza.
     */
    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS);
        }
    }
}
