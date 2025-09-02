package br.com.lumilivre.api.repository;

import java.util.List;
import java.util.Optional;

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

	Optional<CursoModel> findByModulo(String modulo);

	List<CursoModel> findByTurno(Turno turno);

	@Query("""
			    SELECT c FROM CursoModel c
			    WHERE LOWER(c.nome) LIKE LOWER(CONCAT('%', :texto, '%'))
			       OR LOWER(c.turno) LIKE LOWER(CONCAT('%', :texto, '%'))
			       OR STR(c.modulo) LIKE CONCAT('%', :texto, '%')
			       OR LOWER(c.descricao) LIKE LOWER(CONCAT('%', :texto, '%'))
			""")
	Page<CursoModel> buscarPorTexto(@Param("texto") String texto, Pageable pageable);

	@Query("""
			    SELECT c FROM CursoModel c
			    WHERE (:nome IS NULL OR LOWER(c.nome) LIKE LOWER(CONCAT('%', :nome, '%')))
			      AND (:turno IS NULL OR c.turno = :turno)
			      AND (:modulo IS NULL OR c.modulo = :modulo)
			      AND (:descricao IS NULL OR LOWER(c.descricao) LIKE LOWER(CONCAT('%', :descricao, '%')))
			""")
	Page<CursoModel> buscarAvancado(
			@Param("nome") String nome,
			@Param("turno") String turno,
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
