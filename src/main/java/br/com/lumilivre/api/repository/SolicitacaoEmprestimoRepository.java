package br.com.lumilivre.api.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import br.com.lumilivre.api.dto.solicitacao.SolicitacaoDashboardResponse;
import br.com.lumilivre.api.enums.StatusSolicitacao;
import br.com.lumilivre.api.model.SolicitacaoEmprestimoModel;

public interface SolicitacaoEmprestimoRepository extends JpaRepository<SolicitacaoEmprestimoModel, Integer> {

	List<SolicitacaoEmprestimoModel> findByAlunoMatriculaAndStatus(String matricula, StatusSolicitacao status);

	List<SolicitacaoEmprestimoModel> findByStatus(StatusSolicitacao status);

	List<SolicitacaoEmprestimoModel> findAllByOrderByDataSolicitacaoDesc();

	@Query("""
			    SELECT new br.com.lumilivre.api.dto.solicitacao.SolicitacaoDashboardResponse(
			        a.nomeCompleto,
			        l.nome,
			        ex.tombo,
			        s.dataSolicitacao
			    )
			    FROM SolicitacaoEmprestimoModel s
			    JOIN s.aluno a
			    JOIN s.exemplar ex
			    JOIN ex.livro l
			    WHERE s.status = br.com.lumilivre.api.enums.StatusSolicitacao.PENDENTE
			    ORDER BY s.dataSolicitacao ASC
			""")
	List<SolicitacaoDashboardResponse> findSolicitacoesPendentes();

	List<SolicitacaoEmprestimoModel> findByAlunoMatriculaOrderByDataSolicitacaoDesc(String matricula);
}
