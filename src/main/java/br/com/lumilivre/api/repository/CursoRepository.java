package br.com.lumilivre.api.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import br.com.lumilivre.api.data.ListaCursoDTO;
import br.com.lumilivre.api.enums.Turno;
import br.com.lumilivre.api.model.CursoModel;

@Repository
public interface CursoRepository extends JpaRepository<CursoModel, Integer> {

	boolean existsByNomeIgnoreCase(String nome);

	boolean existsByNomeIgnoreCaseAndIdNot(String nome, Integer id);

	List<CursoModel> findByTurno(Turno turno);

	@Query("""
			    SELECT c FROM CursoModel c
			    WHERE c.nome ILIKE :texto
			       OR CAST(c.turno AS text) ILIKE :texto
			       OR c.modulo ILIKE :texto
			""")
	Page<CursoModel> buscarPorTexto(@Param("texto") String texto, Pageable pageable);

	@Query("""
            SELECT c FROM CursoModel c
            WHERE (:nome IS NULL OR c.nome ILIKE :nome)
              AND (:turno IS NULL OR c.turno = :turno)
              AND (:modulo IS NULL OR c.modulo ILIKE :modulo)
        """)
	Page<CursoModel> buscarAvancado(
			@Param("nome") String nome,
			@Param("turno") Turno turno,
			@Param("modulo") String modulo,
			Pageable pageable);

	@Query("""
			SELECT new br.com.lumilivre.api.data.ListaCursoDTO(
			    c.nome,
			    c.turno,
			    c.modulo
			)
			FROM CursoModel c
			ORDER BY c.nome
			""")
	Page<ListaCursoDTO> findCursoParaListaAdmin(Pageable pageable);
}
