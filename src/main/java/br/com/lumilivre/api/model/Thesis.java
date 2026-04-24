package br.com.lumilivre.api.model;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "thesis")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Thesis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "authors", nullable = false, length = 500)
    private String authors;

    @Column(name = "advisors", length = 500)
    private String advisors;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    @ToString.Exclude
    private Course course;

    @Column(name = "completion_year")
    private Integer completionYear;

    @Column(name = "completion_semester", length = 10)
    private String completionSemester;

    @Column(name = "pdf_url", length = 1024)
    private String pdfUrl;

    @Column(name = "cover_url", length = 1024)
    private String coverUrl;

    @Column(name = "external_url", length = 1024)
    private String externalUrl;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean active = true;

    @Builder.Default
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder.Default
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
