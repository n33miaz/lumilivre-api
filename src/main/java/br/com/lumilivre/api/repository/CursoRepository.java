package br.com.lumilivre.api.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import br.com.lumilivre.api.dto.CursoEstatisticasDTO;
import br.com.lumilivre.api.dto.ListaCursoDTO;
import br.com.lumilivre.api.model.CursoModel;

@Repository
public interface CursoRepository extends JpaRepository<CursoModel, Integer> {

	boolean existsByNomeIgnoreCase(String nome);

	boolean existsByNomeIgnoreCaseAndIdNot(String nome, Integer id);

	Optional<CursoModel> findByNomeIgnoreCase(String nome);

	@Query("""
			    SELECT c FROM CursoModel c
			    WHERE c.nome ILIKE :texto
			""")
	Page<CursoModel> buscarPorTexto(@Param("texto") String texto, Pageable pageable);

	@Query("""
			    SELECT c FROM CursoModel c
			    WHERE (:nome IS NULL OR c.nome ILIKE :nome)
			""")
	Page<CursoModel> buscarAvancado(
			@Param("nome") String nome,
			Pageable pageable);

	@Query("""
			SELECT new br.com.lumilivre.api.dto.ListaCursoDTO(
			    c.nome
			)
			FROM CursoModel c
			ORDER BY c.nome
			""")
	Page<ListaCursoDTO> findCursoParaListaAdmin(Pageable pageable);

	@Query("""
			    SELECT new br.com.lumilivre.api.dto.CursoEstatisticasDTO(
			        c.nome,
			        COUNT(a.id),
			        SUM(a.emprestimosCount)
			    )
			    FROM CursoModel c
			    LEFT JOIN c.alunos a
			    GROUP BY c.id, c.nome
			    ORDER BY c.nome
			""")
	List<CursoEstatisticasDTO> findEstatisticasCursos();

	@Query("""
			SELECT new br.com.lumilivre.api.dto.ListaCursoDTO(c.id, c.nome, COUNT(a))
			FROM CursoModel c
			LEFT JOIN c.alunos a
			WHERE (:texto IS NULL OR c.nome ILIKE CONCAT('%', :texto, '%'))
			GROUP BY c.id, c.nome
			""")
	Page<ListaCursoDTO> buscarPorTextoComDTO(@Param("texto") String texto, Pageable pageable);

	@Query("""
			SELECT new br.com.lumilivre.api.dto.ListaCursoDTO(c.id, c.nome, COUNT(a))
			FROM CursoModel c
			LEFT JOIN c.alunos a
			WHERE (:nome IS NULL OR c.nome ILIKE :nome)
			GROUP BY c.id, c.nome
			""")
	Page<ListaCursoDTO> buscarAvancadoComDTO(@Param("nome") String nome, Pageable pageable);

	@Query("""
			SELECT new br.com.lumilivre.api.dto.ListaCursoDTO(c.id, c.nome, COUNT(a))
			FROM CursoModel c
			LEFT JOIN c.alunos a
			WHERE (:texto IS NULL OR c.nome ILIKE CONCAT('%', :texto, '%'))
			GROUP BY c.id, c.nome
			ORDER BY c.nome
			""")
	Page<ListaCursoDTO> findCursoParaListaAdminComFiltro(@Param("texto") String texto, Pageable pageable);
}
