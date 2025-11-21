package br.com.lumilivre.api.model;

import br.com.lumilivre.api.enums.StatusLivro;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "exemplar")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExemplarModel {

	@Id
	@Column(name = "tombo", length = 10, unique = true)
	private String tombo;

	@Enumerated(EnumType.STRING)
	@Column(name = "status_livro", length = 55)
	private StatusLivro status_livro;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "livro_id", nullable = false)
	@ToString.Exclude
	@EqualsAndHashCode.Exclude
	private LivroModel livro;

	@Column(name = "localizacao_fisica", nullable = false)
	private String localizacao_fisica;
}