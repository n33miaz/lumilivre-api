package br.com.lumilivre.api.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "cdd_classificacao")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CddModel {
	@Id
	private String codigo;
	private String descricao;
}