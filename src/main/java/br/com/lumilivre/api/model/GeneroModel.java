package br.com.lumilivre.api.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "genero")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GeneroModel {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@Column(nullable = false, unique = true, length = 100)
	private String nome;
}