package br.com.lumilivre.api.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import br.com.lumilivre.api.enums.LoanRequestStatus;
import br.com.lumilivre.api.model.LoanRequest;

@Repository
public interface LoanRequestRepository extends JpaRepository<LoanRequest, UUID> {

    List<LoanRequest> findByStudent_RegistrationNumberAndStatus(String registrationNumber, LoanRequestStatus status);

    List<LoanRequest> findByStatus(LoanRequestStatus status);

    List<LoanRequest> findAllByOrderByRequestedAtDesc();

    List<LoanRequest> findByStudent_RegistrationNumberOrderByRequestedAtDesc(String registrationNumber);

    long countByStatus(LoanRequestStatus status);
}
