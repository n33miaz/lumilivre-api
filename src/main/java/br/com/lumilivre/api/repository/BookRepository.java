package br.com.lumilivre.api.repository;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import br.com.lumilivre.api.dto.book.BookCardResponse;
import br.com.lumilivre.api.dto.book.BookGroupedResponse;
import br.com.lumilivre.api.dto.book.BookListItemProjection;
import br.com.lumilivre.api.enums.AgeRating;
import br.com.lumilivre.api.enums.BookCopyStatus;
import br.com.lumilivre.api.enums.CoverType;
import br.com.lumilivre.api.model.Book;

@Repository
public interface BookRepository extends JpaRepository<Book, UUID> {

    Optional<Book> findByIsbn(String isbn);

    Optional<Book> findByTitleIgnoreCase(String title);

    boolean existsByIsbn(String isbn);

    void deleteByIsbn(String isbn);

    @Query("SELECT DISTINCT l FROM Book l LEFT JOIN FETCH l.genres LEFT JOIN FETCH l.deweyClassification")
    List<Book> findAllCompleto();

    @Query("""
            SELECT l FROM Book l
            WHERE LOWER(l.title) LIKE LOWER(CONCAT('%', :texto, '%'))
               OR LOWER(l.author) LIKE LOWER(CONCAT('%', :texto, '%'))
               OR LOWER(l.publisher) LIKE LOWER(CONCAT('%', :texto, '%'))
               OR l.isbn LIKE CONCAT('%', :texto, '%')
            """)
    Page<Book> findIdsPorTexto(@Param("texto") String texto, Pageable pageable);

    /**
     * Filtro avancado da tela de livros.
     *
     * <p>Dois detalhes que parecem cosmeticos e nao sao:
     *
     * <p>{@code cast(:dataLancamento as date)} — sem o cast o Postgres recebe um
     * parametro sem tipo no {@code IS NULL} e responde
     * "could not determine data type of parameter", derrubando a busca sempre que
     * a data e preenchida. Mesmo remedio ja aplicado nos {@code findForReport}.
     *
     * <p>{@code COUNT(DISTINCT e.id)} — o LEFT JOIN em generos multiplica as
     * linhas, e um {@code COUNT(e)} contava exemplar x genero: livro com 3
     * exemplares e 2 generos aparecia com 6 exemplares na tela.
     */
    @Query("""
            SELECT new br.com.lumilivre.api.dto.book.BookGroupedResponse(
                l.id,
                l.isbn,
                l.title,
                l.author,
                l.publisher,
                COUNT(DISTINCT e.id)
            )
            FROM Book l
            LEFT JOIN l.copies e
            LEFT JOIN l.genres g
            LEFT JOIN l.deweyClassification c
            WHERE (:nome IS NULL OR LOWER(l.title) LIKE :nome)
              AND (:isbn IS NULL OR l.isbn = :isbn)
              AND (:autor IS NULL OR LOWER(l.author) LIKE :autor)
              AND (:genero IS NULL OR LOWER(g.name) LIKE :genero)
              AND (:editora IS NULL OR LOWER(l.publisher) LIKE :editora)
              AND (:cdd IS NULL OR c.code = :cdd)
              AND (:classificacao IS NULL OR l.ageRating = :classificacao)
              AND (:tipoCapa IS NULL OR l.coverType = :tipoCapa)
              AND (cast(:dataLancamento as date) IS NULL OR l.publicationDate = :dataLancamento)
            GROUP BY l.id, l.isbn, l.title, l.author, l.publisher
            """)
    Page<BookGroupedResponse> buscarAvancado(
            @Param("nome") String nome,
            @Param("isbn") String isbn,
            @Param("autor") String autor,
            @Param("genero") String genero,
            @Param("editora") String editora,
            @Param("cdd") String cdd,
            @Param("classificacao") AgeRating classificacao,
            @Param("tipoCapa") CoverType tipoCapa,
            @Param("dataLancamento") LocalDate dataLancamento,
            Pageable pageable);

    @Query(value = """
            SELECT
                e.status AS status,
                e.copy_code AS copyCode,
                l.isbn AS isbn,
                l.dewey_code AS deweyCode,
                l.title AS title,
                COALESCE((SELECT STRING_AGG(g.name, ', ') FROM genre g JOIN book_genre bg ON g.id = bg.genre_id WHERE bg.book_id = l.id), '') AS genre,
                l.author AS author,
                l.publisher AS publisher,
                e.shelf_location AS physicalLocation
            FROM book_copy e
            JOIN book l ON e.book_id = l.id
            """, countQuery = "SELECT COUNT(*) FROM book_copy", nativeQuery = true)
    Page<BookListItemProjection> findLivrosParaListaAdmin(Pageable pageable);

    @Query("""
            SELECT new br.com.lumilivre.api.dto.book.BookGroupedResponse(
                l.id,
                l.isbn,
                l.title,
                l.author,
                l.publisher,
                COUNT(e)
            )
            FROM Book l
            LEFT JOIN l.copies e
            WHERE (:texto IS NULL OR :texto = ''
               OR LOWER(l.title) LIKE LOWER(CONCAT('%', :texto, '%'))
               OR l.isbn LIKE CONCAT('%', :texto, '%'))
            GROUP BY l.id, l.isbn, l.title, l.author, l.publisher
            """)
    Page<BookGroupedResponse> findLivrosAgrupados(Pageable pageable, @Param("texto") String texto);

    @Query(value = "SELECT l FROM Book l JOIN l.genres g WHERE LOWER(g.name) = LOWER(:nomeGenero)", countQuery = "SELECT count(l) FROM Book l JOIN l.genres g WHERE LOWER(g.name) = LOWER(:nomeGenero)")
    Page<Book> findIdsByGeneroNomeIgnoreCase(@Param("nomeGenero") String nomeGenero, Pageable pageable);

    @Query("SELECT DISTINCT l FROM Book l JOIN FETCH l.genres WHERE l IN :livros")
    List<Book> findWithGeneros(@Param("livros") List<Book> livros);

    @Query("SELECT l FROM Book l JOIN FETCH l.genres WHERE l.id = :id")
    Optional<Book> findByIdWithGeneros(@Param("id") UUID id);

    @Query(value = """
            WITH RankedLivros AS (
                SELECT
                    l.id,
                    l.title AS title,
                    l.author AS author,
                    l.cover_url AS coverUrl,
                    l.rating AS rating,
                    l.updated_at AS updatedAt,
                    g.name AS genreName,
                    ROW_NUMBER() OVER(PARTITION BY g.name ORDER BY l.publication_date DESC, l.id DESC) as rn
                FROM book l
                JOIN book_genre bg ON l.id = bg.book_id
                JOIN genre g ON bg.genre_id = g.id
            )
            SELECT
                id,
                title,
                author,
                coverUrl,
                rating,
                updatedAt,
                genreName
            FROM RankedLivros
            WHERE rn <= 10
            ORDER BY genreName, rn
            """, nativeQuery = true)
    List<Map<String, Object>> findCatalogoMobile();

    @Query("""
            SELECT DISTINCT l FROM Book l
            LEFT JOIN FETCH l.genres g
            LEFT JOIN FETCH l.deweyClassification c
            WHERE (:genero IS NULL OR g.name ILIKE :genero)
              AND (:autor IS NULL OR l.author ILIKE :autor)
              AND (:editora IS NULL OR l.publisher ILIKE :editora)
              AND (:cdd IS NULL OR c.code = :cdd)
              AND (:classificacaoEtaria IS NULL OR CAST(l.ageRating AS text) = :classificacaoEtaria)
              AND (:tipoCapa IS NULL OR CAST(l.coverType AS text) = :tipoCapa)
              AND (cast(:inicio as timestamp) IS NULL OR l.createdAt >= :inicio)
              AND (cast(:fim as timestamp) IS NULL OR l.createdAt <= :fim)
            ORDER BY l.title
            """)
    List<Book> findForReport(
            @Param("genero") String genero,
            @Param("autor") String autor,
            @Param("editora") String editora,
            @Param("cdd") String cdd,
            @Param("classificacaoEtaria") String classificacaoEtaria,
            @Param("tipoCapa") String tipoCapa,
            @Param("inicio") OffsetDateTime inicio,
            @Param("fim") OffsetDateTime fim);

    @Query("""
            SELECT l.author as autor, COUNT(l.id) as total
            FROM Book l
            WHERE l.author IS NOT NULL
            GROUP BY l.author
            ORDER BY total DESC
            """)
    List<Map<String, Object>> countByAutor();

    @Query("""
            SELECT g.name as genero, COUNT(l.id) as total
            FROM Book l
            JOIN l.genres g
            GROUP BY g.name
            ORDER BY total DESC
            """)
    List<Map<String, Object>> countByGenero();

    @Query("""
            SELECT l FROM Book l
            LEFT JOIN FETCH l.genres
            LEFT JOIN FETCH l.deweyClassification
            WHERE l.id = :id
            """)
    Optional<Book> findByIdWithDetails(@Param("id") UUID id);

    @Query("SELECT COUNT(e) FROM BookCopy e WHERE e.book.id = :bookId AND e.status = :status")
    long countCopiesByStatus(@Param("bookId") UUID bookId, @Param("status") BookCopyStatus status);

    /**
     * Navegacao por genero do app.
     *
     * <p>Sem {@code ORDER BY} de proposito: a ordem vem do {@link Pageable} que
     * o {@code BookService} monta, e ela e sempre total (ver
     * {@code GENERO_SORT}). Fixar a ordem aqui teria efeito contrario ao
     * esperado — em JPQL o Spring Data <b>anexa</b> o sort do cliente a clausula
     * existente, entao o {@code ORDER BY} do arquivo passaria na frente e o
     * campo pedido pelo cliente viraria apenas criterio de desempate.
     */
    @Query("""
            SELECT new br.com.lumilivre.api.dto.book.BookCardResponse(
                l.id,
                l.title,
                l.author,
                l.coverUrl,
                l.rating,
                l.updatedAt
            )
            FROM Book l
            JOIN l.genres g
            WHERE LOWER(g.name) = LOWER(:nomeGenero)
            """)
    Page<BookCardResponse> findByGeneroAsCatalogoDTO(@Param("nomeGenero") String nomeGenero, Pageable pageable);

    @Query("""
            SELECT new br.com.lumilivre.api.dto.book.BookCardResponse(
                l.id,
                COALESCE(l.title, ''),
                COALESCE(l.author, 'Unknown Author'),
                COALESCE(l.coverUrl, ''),
                COALESCE(l.rating, 0.0),
                l.updatedAt
            )
            FROM Book l
            WHERE LOWER(l.title) LIKE LOWER(CONCAT('%', :texto, '%'))
               OR LOWER(l.author) LIKE LOWER(CONCAT('%', :texto, '%'))
            """)
    Page<BookCardResponse> buscarMobilePorTexto(@Param("texto") String texto, Pageable pageable);

    @Query("""
            SELECT new br.com.lumilivre.api.dto.book.BookCardResponse(
                l.id,
                COALESCE(l.title, ''),
                COALESCE(l.author, 'Unknown Author'),
                COALESCE(l.coverUrl, ''),
                COALESCE(l.rating, 0.0),
                l.updatedAt
            )
            FROM Book l JOIN l.genres g
            WHERE LOWER(g.name) IN :generos
              AND l.id NOT IN :jaLidos
            ORDER BY l.rating DESC NULLS LAST
            """)
    List<BookCardResponse> findRecomendacoesPorGenero(
            @Param("generos") List<String> generos,
            @Param("jaLidos") List<UUID> jaLidos,
            Pageable pageable);

    @Query("""
            SELECT new br.com.lumilivre.api.dto.book.BookCardResponse(
                l.id,
                COALESCE(l.title, ''),
                COALESCE(l.author, 'Unknown Author'),
                COALESCE(l.coverUrl, ''),
                COALESCE(l.rating, 0.0),
                l.updatedAt
            )
            FROM Book l
            ORDER BY l.rating DESC NULLS LAST
            """)
    List<BookCardResponse> findTopRated(Pageable pageable);
}
