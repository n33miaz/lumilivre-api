package br.com.lumilivre.api.model;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;

import br.com.lumilivre.api.enums.AgeRating;
import br.com.lumilivre.api.enums.CoverType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
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

@Entity(name = "Book")
@Table(name = "book")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "uuid")
    private UUID id;

    @Column(name = "isbn", length = 20)
    private String isbn;

    @NotNull
    @Column(name = "title", length = 255, nullable = false)
    private String title;

    @Column(name = "publication_date")
    private LocalDate publicationDate;

    @Column(name = "page_count")
    private Integer pageCount;

    @ManyToOne
    @JoinColumn(name = "dewey_code")
    @ToString.Exclude
    private DeweyClassification deweyClassification;

    @NotNull
    @Column(name = "publisher", length = 120, nullable = false)
    private String publisher;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "age_rating", length = 30, nullable = false)
    private AgeRating ageRating;

    @Column(name = "edition", length = 55)
    private String edition;

    @Column(name = "volume")
    private Integer volume;

    @Column(name = "synopsis", columnDefinition = "TEXT")
    private String synopsis;

    @Column(name = "author", length = 255)
    private String author;

    @Enumerated(EnumType.STRING)
    @Column(name = "cover_type", length = 30)
    private CoverType coverType;

    @Column(name = "cover_url", length = 1024)
    private String coverUrl;

    @Builder.Default
    @Column(name = "rating")
    private Double rating = 4.6;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    @OneToMany(mappedBy = "book")
    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<BookCopy> copies;

    @ManyToMany
    @JoinTable(
        name = "book_genre",
        joinColumns = @JoinColumn(name = "book_id"),
        inverseJoinColumns = @JoinColumn(name = "genre_id")
    )
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Builder.Default
    private Set<Genre> genres = new HashSet<>();

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
