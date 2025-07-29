package br.com.lumilivre.api.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import br.com.lumilivre.api.enums.StatusEmprestimo;
import br.com.lumilivre.api.model.AlunoModel;
import br.com.lumilivre.api.model.EmprestimoModel;
import br.com.lumilivre.api.model.ExemplarModel;

@Repository
public interface EmprestimoRepository extends JpaRepository <EmprestimoModel, Integer>{
    boolean existsByExemplarTomboAndStatusEmprestimo(String tombo, StatusEmprestimo status);
    List<EmprestimoModel> findByStatusEmprestimo(StatusEmprestimo status);

    List<EmprestimoModel> findByStatusEmprestimoAndDataDevolucaoBefore(StatusEmprestimo status, LocalDateTime now);

    List<EmprestimoModel> findByStatusEmprestimoAndDataDevolucaoGreaterThanEqual(StatusEmprestimo status, LocalDateTime now);


}
