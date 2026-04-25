package br.com.lumilivre.api.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import br.com.lumilivre.api.dto.solicitacao.SolicitacaoDashboardResponse;
import br.com.lumilivre.api.enums.LoanRequestStatus;
import br.com.lumilivre.api.model.LoanRequest;

@Repository
public interface LoanRequestRepository extends JpaRepository<LoanRequest, UUID> {

    List<LoanRequest> findByStudent_RegistrationNumberAndStatus(String registrationNumber, LoanRequestStatus status);

    List<LoanRequest> findByStatus(LoanRequestStatus status);

    List<LoanRequest> findAllByOrderByRequestedAtDesc();

    @Query("""
            SELECT new br.com.lumilivre.api.dto.solicitacao.SolicitacaoDashboardResponse(
                a.fullName,
                l.title,
                ex.copyCode,
                s.requestedAt
            )
            FROM LoanRequest s
            JOIN s.student a
            JOIN s.bookCopy ex
            JOIN ex.book l
            WHERE s.status = br.com.lumilivre.api.enums.LoanRequestStatus.PENDING
            ORDER BY s.requestedAt ASC
            """)
    List<SolicitacaoDashboardResponse> findSolicitacoesPendentes();

    List<LoanRequest> findByStudent_RegistrationNumberOrderByRequestedAtDesc(String registrationNumber);

    long countByStatus(LoanRequestStatus status);
}
