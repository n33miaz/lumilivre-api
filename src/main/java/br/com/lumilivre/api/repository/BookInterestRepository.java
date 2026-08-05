package br.com.lumilivre.api.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import br.com.lumilivre.api.dto.book.BookInterestSummaryResponse;
import br.com.lumilivre.api.enums.BookCopyStatus;
import br.com.lumilivre.api.model.BookInterest;

@Repository
public interface BookInterestRepository extends JpaRepository<BookInterest, UUID> {

    Optional<BookInterest> findByReader_IdAndBook_Id(UUID readerId, UUID bookId);

    /**
     * Apaga o interesse e diz quantas linhas caíram, para o endpoint distinguir
     * "estava marcado e desmarquei" de "não estava marcado" sem um SELECT antes.
     */
    @Modifying
    @Query("DELETE FROM BookInterest i WHERE i.reader.id = :readerId AND i.book.id = :bookId")
    int deleteByReaderAndBook(@Param("readerId") UUID readerId, @Param("bookId") UUID bookId);

    /**
     * A lista do próprio leitor. {@code JOIN FETCH} do livro porque o card
     * precisa de título, autor e capa: sem ele seriam N selects para uma página
     * de N interesses.
     *
     * <p>O {@code LEFT JOIN FETCH} da classificação CDD não é excesso de zelo, é
     * o N+1 de verdade: {@code Book.deweyClassification} é um
     * {@code @ManyToOne} e o default do JPA é EAGER, então cada livro trazido
     * disparava um {@code select ... from dewey_classification} próprio — foi o
     * que apareceu no log de SQL do Postgres, dois selects extras para dois
     * interesses, vinte para uma página cheia. O card nem usa o CDD; o remédio é
     * trazê-lo no mesmo round-trip, como o {@code findByIdWithDetails} já faz.
     *
     * <p>Ordena pelos mais recentes e desempata por id — {@code created_at} tem
     * empate real (marcar vários livros na mesma tela cai no mesmo instante em
     * ambiente rápido) e paginar sem ordem total repete ou pula linhas.
     */
    @Query(value = """
            SELECT i FROM BookInterest i
            JOIN FETCH i.book b
            LEFT JOIN FETCH b.deweyClassification
            WHERE i.reader.id = :readerId
            ORDER BY i.createdAt DESC, i.id DESC
            """,
            countQuery = "SELECT COUNT(i) FROM BookInterest i WHERE i.reader.id = :readerId")
    Page<BookInterest> findMine(@Param("readerId") UUID readerId, Pageable pageable);

    /**
     * O indicador da biblioteca: um livro por linha, com quantos leitores o
     * querem e quantos exemplares existem.
     *
     * <p><b>Uma consulta só, nunca N+1.</b> A disponibilidade sai do mesmo
     * {@code LEFT JOIN} dos exemplares, com {@code CASE} dentro do
     * {@code COUNT}, em vez de uma contagem por livro depois de paginar. O
     * {@code DISTINCT} não é decoração: o join com exemplares multiplica as
     * linhas de interesse, e um {@code COUNT(i)} contaria interesse × exemplar
     * — o mesmo defeito que a {@code buscarAvancado} já teve.
     *
     * <p>{@code maxAvailableCopies} é o filtro "só o que não conseguimos
     * atender" em forma de teto numérico. É um teto e não um booleano de
     * propósito: parâmetro booleano solto dentro de {@code HAVING} chega no
     * Postgres sem tipo e derruba a consulta com
     * "could not determine data type of parameter" — o mesmo tropeço que já
     * obrigou os {@code cast(...)} do {@code buscarAvancado}. Comparado com um
     * {@code COUNT}, o tipo é inferido do operador. O serviço passa 0 para
     * "nenhum exemplar disponível" e {@link Long#MAX_VALUE} para "tudo".
     *
     * <p>A ordem é fixa na consulta (mais desejado primeiro, e entre iguais o
     * que a biblioteca menos consegue atender) porque este endpoint responde uma
     * pergunta só. O {@code sort} do cliente é descartado no serviço: em JPQL o
     * Spring Data anexa {@code ORDER BY alias.propriedade} ao final, o que aqui
     * daria 500 para qualquer campo e, no melhor caso, empurraria a ordem que
     * dá sentido à página para depois da ordem do cliente.
     */
    @Query(value = """
            SELECT new br.com.lumilivre.api.dto.book.BookInterestSummaryResponse(
                b.id,
                b.title,
                b.author,
                b.coverUrl,
                b.updatedAt,
                COUNT(DISTINCT i.id),
                COUNT(DISTINCT c.id),
                COUNT(DISTINCT CASE WHEN c.status = :available THEN c.id END)
            )
            FROM BookInterest i
            JOIN i.book b
            LEFT JOIN b.copies c
            GROUP BY b.id, b.title, b.author, b.coverUrl, b.updatedAt
            HAVING COUNT(DISTINCT CASE WHEN c.status = :available THEN c.id END) <= :maxAvailableCopies
            ORDER BY COUNT(DISTINCT i.id) DESC,
                     COUNT(DISTINCT CASE WHEN c.status = :available THEN c.id END) ASC,
                     b.title ASC,
                     b.id ASC
            """,
            countQuery = """
            SELECT COUNT(b.id) FROM Book b
            WHERE EXISTS (SELECT 1 FROM BookInterest i WHERE i.book = b)
              AND (SELECT COUNT(c.id) FROM BookCopy c
                   WHERE c.book = b AND c.status = :available) <= :maxAvailableCopies
            """)
    Page<BookInterestSummaryResponse> summarize(
            @Param("available") BookCopyStatus available,
            @Param("maxAvailableCopies") long maxAvailableCopies,
            Pageable pageable);
}
