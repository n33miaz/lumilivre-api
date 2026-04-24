package br.com.lumilivre.api.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "dewey_classification")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeweyClassification {

    @Id
    @Column(name = "code", length = 20)
    private String code;

    @Column(name = "description", length = 255)
    private String description;
}
