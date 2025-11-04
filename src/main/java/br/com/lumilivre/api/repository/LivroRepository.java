package br.com.lumilivre.api.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import br.com.lumilivre.api.data.ListaLivroDTO;
import br.com.lumilivre.api.data.LivroAgrupadoDTO;
import br.com.lumilivre.api.model.LivroModel;

@Repository
public interface LivroRepository extends JpaRepository<LivroModel, Long> {

    Optional<LivroModel> findByIsbn(String isbn);

    Optional<LivroModel> findByNomeIgnoreCase(String nome);

    boolean existsByIsbn(String isbn);

    void deleteByIsbn(String isbn);

    @Query("SELECT DISTINCT l FROM LivroModel l JOIN l.exemplares e WHERE e.status_livro = 'DISPONIVEL'")
    List<LivroModel> findLivrosDisponiveis();

    @Query("""
                SELECT DISTINCT l FROM LivroModel l
                LEFT JOIN l.generos g
                WHERE LOWER(l.nome) LIKE LOWER(CONCAT('%', :texto, '%'))
                OR LOWER(l.sinopse) LIKE LOWER(CONCAT('%', :texto, '%'))
                OR LOWER(l.autor) LIKE LOWER(CONCAT('%', :texto, '%'))
                OR LOWER(g.nome) LIKE LOWER(CONCAT('%', :texto, '%'))
                OR LOWER(l.editora) LIKE LOWER(CONCAT('%', :texto, '%'))
            """)
    Page<LivroModel> buscarPorTexto(@Param("texto") String texto, Pageable pageable);

    @Query("""
                SELECT DISTINCT l FROM LivroModel l
                LEFT JOIN l.generos g
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

    @Query("""
                SELECT new br.com.lumilivre.api.data.ListaLivroDTO(
                    e.status_livro,
                    e.tombo,
                    l.isbn,
                    l.cdd.codigo,
                    l.nome,
                    STRING_AGG(g.nome, ', '),
                    l.autor,
                    l.editora,
                    e.localizacao_fisica
                )
                FROM LivroModel l
                JOIN l.exemplares e
                LEFT JOIN l.generos g
                GROUP BY e.status_livro, e.tombo, l.isbn, l.cdd, l.nome, l.autor, l.editora, e.localizacao_fisica
                ORDER BY l.nome
            """)
    Page<ListaLivroDTO> findLivrosParaListaAdmin(Pageable pageable);

    @Query("SELECT l FROM LivroModel l JOIN l.generos g WHERE LOWER(g.nome) = LOWER(:nomeGenero)")
    List<LivroModel> findByGeneroNomeIgnoreCase(@Param("nomeGenero") String nomeGenero);

    @Query("""
            SELECT new br.com.lumilivre.api.data.LivroAgrupadoDTO(
                l.isbn,
                l.nome,
                l.autor,
                l.editora,
                COUNT(e)
            )
            FROM LivroModel l
            LEFT JOIN l.exemplares e
            WHERE (:texto IS NULL OR l.nome ILIKE CONCAT('%', :texto, '%') OR l.isbn ILIKE CONCAT('%', :texto, '%'))
            GROUP BY l.isbn, l.nome, l.autor, l.editora
            """)
    Page<LivroAgrupadoDTO> findLivrosAgrupados(Pageable pageable, @Param("texto") String texto);

    @Query("SELECT l FROM LivroModel l JOIN l.generos g WHERE LOWER(g.nome) = LOWER(:nomeGenero)")
    Page<LivroModel> findByGeneroNomeIgnoreCase(@Param("nomeGenero") String nomeGenero, Pageable pageable);
}