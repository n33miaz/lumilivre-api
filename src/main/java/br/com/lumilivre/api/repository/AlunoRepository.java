package br.com.lumilivre.api.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.lumilivre.api.data.ListaAlunoDTO;
import br.com.lumilivre.api.enums.Penalidade;
import br.com.lumilivre.api.model.AlunoModel;

public interface AlunoRepository extends JpaRepository<AlunoModel, String> {

	Optional<AlunoModel> findByMatricula(String matricula);

	Optional<AlunoModel> findByCpf(String cpf);

	Optional<AlunoModel> findByNomeIgnoreCase(String nome);

	List<AlunoModel> findAllByOrderByEmprestimosCountDesc();

	@Query(value = """
			    SELECT *
			    FROM aluno a
			    WHERE a.texto_busca @@ plainto_tsquery('portuguese', :texto)
			""", nativeQuery = true)
	Page<AlunoModel> buscarPorTexto(@Param("texto") String texto, Pageable pageable);

	@Query("""
			    SELECT a FROM AlunoModel a
			    LEFT JOIN a.curso c
			    WHERE (:penalidadeEnum IS NULL OR a.penalidade = :penalidadeEnum)
			      AND (:matricula IS NULL OR a.matricula = :matricula)
			      AND (:nome IS NULL OR LOWER(a.nome) LIKE CONCAT('%', LOWER(:nome), '%'))
			      AND (:cursoNome IS NULL OR LOWER(c.nome) LIKE CONCAT('%', LOWER(:cursoNome), '%'))
			      AND (:dataNascimento IS NULL OR a.dataNascimento = :dataNascimento)
			      AND (:email IS NULL OR LOWER(a.email) LIKE CONCAT('%', LOWER(:email), '%'))
			      AND (:celular IS NULL OR a.celular = :celular)
			""")
	Page<AlunoModel> buscarAvancado(
			@Param("penalidadeEnum") Penalidade penalidadeEnum,
			@Param("matricula") String matricula,
			@Param("nome") String nome,
			@Param("cursoNome") String cursoNome,
			@Param("dataNascimento") LocalDate dataNascimento,
			@Param("email") String email,
			@Param("celular") String celular,
			Pageable pageable);

	// busca a lista de alunos SEM filtro
	@Query("""
			    SELECT new br.com.lumilivre.api.data.ListaAlunoDTO(
			        a.penalidade,
			        a.matricula,
			        a.nome,
			        a.email,
			        a.celular,
			        c.nome
			    )
			    FROM AlunoModel a
			    JOIN a.curso c
			    ORDER BY a.nome
			""")
	Page<ListaAlunoDTO> findAlunosParaListaAdmin(Pageable pageable); // Nome do método corrigido

	// busca a lista de alunos COM filtro
	@Query("""
	        SELECT new br.com.lumilivre.api.data.ListaAlunoDTO(
	            a.penalidade, a.matricula, a.nome, a.email, a.celular, c.nome
	        )
	        FROM AlunoModel a JOIN a.curso c
	        WHERE LOWER(a.nome) LIKE LOWER(CONCAT('%', :texto, '%'))
	           OR a.matricula LIKE CONCAT('%', :texto, '%')
	           OR LOWER(c.nome) LIKE LOWER(CONCAT('%', :texto, '%'))
	        ORDER BY a.nome
	    """)
	Page<ListaAlunoDTO> findAlunosParaListaAdminComFiltro(@Param("texto") String texto, Pageable pageable);
}