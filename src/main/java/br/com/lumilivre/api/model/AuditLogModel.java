package br.com.lumilivre.api.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "audit_log")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Identificação do caller: matricula ou username */
    @Column(nullable = false, length = 100)
    private String actor;

    @Column(nullable = false, length = 50)
    private String actorRole;

    /** Valor do parâmetro principal da operação (ex: matricula do aluno, id do empréstimo) */
    @Column(length = 200)
    private String targetId;

    /** Nome da ação auditada — ex: LOAN_CREATED, STUDENT_UPDATED */
    @Column(nullable = false, length = 100)
    private String action;

    /** Resultado: SUCCESS ou FAILURE */
    @Column(nullable = false, length = 20)
    private String result;

    /** Mensagem de erro em caso de falha */
    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @Column(nullable = false)
    private LocalDateTime occurredAt;
}
