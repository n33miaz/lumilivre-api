package br.com.lumilivre.api.repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import br.com.lumilivre.api.data.ListaLivroDTO;
import br.com.lumilivre.api.data.ListaLivroProjection;
import br.com.lumilivre.api.data.LivroAgrupadoDTO;
import br.com.lumilivre.api.model.LivroModel;

@Repository
public interface LivroRepository extends JpaRepository<LivroModel, Long> {

    Optional<LivroModel> findByIsbn(String isbn);

    Optional<LivroModel> findByNomeIgnoreCase(String nome);

    boolean existsByIsbn(String isbn);

    void deleteByIsbn(String isbn);

    @Query("SELECT DISTINCT l FROM LivroModel l JOIN FETCH l.generos g JOIN l.exemplares e")
    List<LivroModel> findLivrosDisponiveis();

    @Query(value = """
                SELECT DISTINCT l FROM LivroModel l
                LEFT JOIN l.generos g
                WHERE LOWER(l.nome) LIKE LOWER(CONCAT('%', :texto, '%'))
                OR LOWER(l.sinopse) LIKE LOWER(CONCAT('%', :texto, '%'))
                OR LOWER(l.autor) LIKE LOWER(CONCAT('%', :texto, '%'))
                OR LOWER(g.nome) LIKE LOWER(CONCAT('%', :texto, '%'))
                OR LOWER(l.editora) LIKE LOWER(CONCAT('%', :texto, '%'))
            """, countQuery = """
                SELECT COUNT(DISTINCT l) FROM LivroModel l
                LEFT JOIN l.generos g
                WHERE LOWER(l.nome) LIKE LOWER(CONCAT('%', :texto, '%'))
                OR LOWER(l.sinopse) LIKE LOWER(CONCAT('%', :texto, '%'))
                OR LOWER(l.autor) LIKE LOWER(CONCAT('%', :texto, '%'))
                OR LOWER(g.nome) LIKE LOWER(CONCAT('%', :texto, '%'))
                OR LOWER(l.editora) LIKE LOWER(CONCAT('%', :texto, '%'))
            """)
    Page<LivroModel> findIdsPorTexto(@Param("texto") String texto, Pageable pageable);

    @Query("""
                SELECT DISTINCT l FROM LivroModel l
                LEFT JOIN FETCH l.generos g
                WHERE (:nome IS NULL OR LOWER(l.nome) LIKE LOWER(CONCAT('%', :nome, '%')))
                  AND (:isbn IS NULL OR l.isbn = :isbn)
                  AND (:autor IS NULL OR LOWER(l.autor) LIKE LOWER(CONCAT('%', :autor, '%')))
                  AND (:genero IS NULL OR LOWER(g.nome) LIKE LOWER(CONCAT('%', :genero, '%')))
                  AND (:editora IS NULL OR LOWER(l.editora) LIKE LOWER(CONCAT('%', :editora, '%')))
            """)
    Page<LivroModel> buscarAvancado(
            @Param("nome") String nome,
            @Param("isbn") String isbn,
            @Param("autor") String autor,
            @Param("genero") String genero,
            @Param("editora") String editora,
            Pageable pageable);

    @Query("""
            SELECT new br.com.lumilivre.api.data.ListaLivroDTO(
                l.nome,
                l.isbn,
                l.nome,
                l.editora,
                l.quantidade
            )
            FROM LivroModel l
            WHERE LOWER(l.nome) LIKE LOWER(CONCAT('%', :texto, '%'))
               OR LOWER(l.autor) LIKE LOWER(CONCAT('%', :texto, '%'))
               OR LOWER(l.editora) LIKE LOWER(CONCAT('%', :texto, '%'))
            ORDER BY l.nome
            """)
    Page<ListaLivroDTO> findLivrosParaListaAdminComFiltro(@Param("texto") String texto, Pageable pageable);

    @Query(value = """
                SELECT
                    e.status_livro AS status,
                    e.tombo AS tomboExemplar,
                    l.isbn AS isbn,
                    l.cdd_codigo AS cdd,
                    l.nome AS nome,
                    COALESCE((SELECT STRING_AGG(g.nome, ', ') FROM genero g JOIN livro_genero lg ON g.id = lg.genero_id WHERE lg.livro_id = l.id), '') AS genero,
                    l.autor AS autor,
                    l.editora AS editora,
                    e.localizacao_fisica AS localizacao_fisica
                FROM exemplar e
                JOIN livro l ON e.livro_id = l.id
            """, countQuery = "SELECT COUNT(*) FROM exemplar", nativeQuery = true)
    Page<ListaLivroProjection> findLivrosParaListaAdmin(Pageable pageable);

    @Query("""
            SELECT new br.com.lumilivre.api.data.LivroAgrupadoDTO(
                l.id,
                l.isbn,
                l.nome,
                l.autor,
                l.editora,
                COUNT(e)
            )
            FROM LivroModel l
            LEFT JOIN l.exemplares e
            WHERE (:texto IS NULL OR l.nome ILIKE CONCAT('%', :texto, '%') OR l.isbn ILIKE CONCAT('%', :texto, '%'))
            GROUP BY l.id, l.isbn, l.nome, l.autor, l.editora
            """)
    Page<LivroAgrupadoDTO> findLivrosAgrupados(Pageable pageable, @Param("texto") String texto);

    @Query(value = "SELECT l FROM LivroModel l JOIN l.generos g WHERE LOWER(g.nome) = LOWER(:nomeGenero)", countQuery = "SELECT count(l) FROM LivroModel l JOIN l.generos g WHERE LOWER(g.nome) = LOWER(:nomeGenero)")
    Page<LivroModel> findIdsByGeneroNomeIgnoreCase(@Param("nomeGenero") String nomeGenero, Pageable pageable);

    @Query("SELECT DISTINCT l FROM LivroModel l JOIN FETCH l.generos WHERE l IN :livros")
    List<LivroModel> findWithGeneros(@Param("livros") List<LivroModel> livros);

    @Query("SELECT l FROM LivroModel l JOIN FETCH l.generos WHERE l.id = :id")
    Optional<LivroModel> findByIdWithGeneros(@Param("id") Long id);

    @Query(value = """
                WITH RankedLivros AS (
                    SELECT
                        l.id,
                        l.nome,
                        l.autor,
                        l.imagem,
                        g.nome AS genero_nome,
                        ROW_NUMBER() OVER(PARTITION BY g.nome ORDER BY l.data_lancamento DESC, l.id DESC) as rn
                    FROM livro l
                    JOIN livro_genero lg ON l.id = lg.livro_id
                    JOIN genero g ON lg.genero_id = g.id
                    WHERE EXISTS (SELECT 1 FROM exemplar e WHERE e.livro_id = l.id)
                )
                SELECT
                    id,
                    nome,
                    autor,
                    imagem,
                    genero_nome
                FROM RankedLivros
                WHERE rn <= 10
                ORDER BY genero_nome, rn
            """, nativeQuery = true)
    List<Map<String, Object>> findCatalogoMobile();

    @Query("""
                SELECT l FROM LivroModel l
                LEFT JOIN l.generos g
                LEFT JOIN l.cdd c
                WHERE (:genero IS NULL OR g.nome ILIKE :genero)
                  AND (:autor IS NULL OR l.autor ILIKE %:autor%)
                  AND (:cdd IS NULL OR c.codigo = :cdd)
                  AND (:classificacaoEtaria IS NULL OR CAST(l.classificacao_etaria AS text) ILIKE :classificacaoEtaria)
                  AND (:tipoCapa IS NULL OR CAST(l.tipo_capa AS text) ILIKE :tipoCapa)
                GROUP BY l.id
                ORDER BY l.nome
            """)
    List<LivroModel> findForReport(
            @Param("genero") String genero,
            @Param("autor") String autor,
            @Param("cdd") String cdd,
            @Param("classificacaoEtaria") String classificacaoEtaria,
            @Param("tipoCapa") String tipoCapa);

    @Query("""
                SELECT l.autor as autor, COUNT(l.id) as total
                FROM LivroModel l
                WHERE l.autor IS NOT NULL
                GROUP BY l.autor
                ORDER BY total DESC
            """)
    List<Map<String, Object>> countByAutor();

    @Query("""
                SELECT g.nome as genero, COUNT(l.id) as total
                FROM LivroModel l
                JOIN l.generos g
                GROUP BY g.nome
                ORDER BY total DESC
            """)
    List<Map<String, Object>> countByGenero();
}