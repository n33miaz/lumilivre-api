package br.com.lumilivre.api.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.lumilivre.api.data.ListaGeneroDTO;
import br.com.lumilivre.api.model.GeneroModel;

public interface GeneroRepository extends JpaRepository<GeneroModel, Integer> {
	boolean existsByNomeIgnoreCase(String nome);

	boolean existsByNomeIgnoreCaseAndIdNot(String nome, Integer id);

	GeneroModel findByNomeIgnoreCase(String nome);

	@Query("""
			    SELECT g FROM GeneroModel g
			    WHERE LOWER(g.nome) LIKE LOWER(CONCAT('%', :texto, '%'))
			""")
	Page<GeneroModel> buscarPorTexto(@Param("texto") String texto, Pageable pageable);

	@Query("""
			    SELECT g FROM GeneroModel g
			    WHERE (:nome IS NULL OR LOWER(g.nome) LIKE LOWER(CONCAT('%', :nome, '%')))
				AND (:id IS NULL OR g.id = :id)
			""")
	Page<GeneroModel> buscarAvancado(
			@Param("id") Integer id,
			@Param("nome") String nome,
			Pageable pageable);
	
    
    @Query("""
    	      SELECT new br.com.lumilivre.api.data.ListaGeneroDTO(
    	          g.id,
    	          g.nome

    	      )
    	      FROM GeneroModel g
    	      ORDER BY g.nome
    	      """)
    	  Page<ListaGeneroDTO> findGeneroParaListaAdmin(Pageable pageable);
}
