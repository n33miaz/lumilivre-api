package br.com.lumilivre.api.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.lumilivre.api.model.CursoModel;
import br.com.lumilivre.api.model.GeneroModel;

public interface GeneroRepository extends JpaRepository<GeneroModel, Integer> {
	boolean existsByNomeIgnoreCase(String nome);

	boolean existsByNomeIgnoreCaseAndIdNot(String nome, Integer id);

	GeneroModel findByNomeIgnoreCase(String nome);

	Optional<GeneroModel> findById(Integer id);

	@Query("""
			    SELECT g FROM GeneroModel g
			    WHERE LOWER(g.nome) LIKE LOWER(CONCAT('%', :texto, '%'))
			""")
	Page<CursoModel> buscarPorTexto(@Param("texto") String texto, Pageable pageable);

	@Query("""
			    SELECT g FROM GeneroModel g
			    WHERE (:nome IS NULL OR LOWER(g.nome) LIKE LOWER(CONCAT('%', :nome, '%')))
			""")
	Page<CursoModel> buscarAvancado(
			@Param("nome") String nome,
			Pageable pageable);
}
