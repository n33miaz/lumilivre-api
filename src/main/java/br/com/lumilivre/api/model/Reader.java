package br.com.lumilivre.api.model;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import br.com.lumilivre.api.enums.PenaltyCode;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity(name = "Reader")
@Table(name = "reader")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reader {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "uuid")
    private UUID id;

    @Column(name = "registration_number", nullable = false, length = 20, unique = true)
    private String registrationNumber;

    @Column(name = "full_name", nullable = false, length = 255)
    private String fullName;

    @Column(name = "avatar_url", length = 1024)
    private String avatarUrl;

    @Column(name = "cpf", length = 11)
    private String cpf;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(name = "email", length = 255, columnDefinition = "citext")
    private String email;

    @ManyToOne
    @JoinColumn(name = "course_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Course course;

    @ManyToOne
    @JoinColumn(name = "study_shift_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private StudyShift studyShift;

    @ManyToOne
    @JoinColumn(name = "academic_module_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private AcademicModule academicModule;

    @Column(name = "reader_category", length = 80)
    private String readerCategory;

    @Column(name = "postal_code", length = 8)
    private String postalCode;

    @Column(name = "street", length = 255)
    private String street;

    @Column(name = "address_complement", length = 55)
    private String addressComplement;

    @Column(name = "district", length = 55)
    private String district;

    @Column(name = "city", length = 55)
    private String city;

    @Column(name = "state_code", length = 2, columnDefinition = "bpchar")
    private String stateCode;

    @Column(name = "street_number")
    private Integer streetNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "penalty_code", length = 20)
    private PenaltyCode penaltyCode;

    @Column(name = "penalty_expires_at")
    private OffsetDateTime penaltyExpiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    @OneToOne(mappedBy = "reader", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private AppUser appUser;

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
