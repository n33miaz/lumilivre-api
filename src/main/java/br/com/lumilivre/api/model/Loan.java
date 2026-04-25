package br.com.lumilivre.api.model;

import java.time.OffsetDateTime;
import java.util.UUID;

import br.com.lumilivre.api.enums.LoanStatus;
import br.com.lumilivre.api.enums.PenaltyCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity(name = "Loan")
@Table(name = "loan")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Loan {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "uuid")
    private UUID id;

    @NotNull
    @Column(name = "borrowed_at", nullable = false)
    private OffsetDateTime borrowedAt;

    @NotNull
    @Column(name = "due_at", nullable = false)
    private OffsetDateTime dueAt;

    @Column(name = "returned_at")
    private OffsetDateTime returnedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "penalty_code", length = 20)
    private PenaltyCode penaltyCode;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private LoanStatus status = LoanStatus.ACTIVE;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "student_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Student student;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "book_copy_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private BookCopy bookCopy;

    @Builder.Default
    @Column(name = "renewal_count", nullable = false)
    private int renewalCount = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    void prePersist() {
        OffsetDateTime now = OffsetDateTime.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
