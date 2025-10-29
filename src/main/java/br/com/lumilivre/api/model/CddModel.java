package br.com.lumilivre.api.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "cdd_classificacao")
@Getter
@Setter
public class CddModel {
    @Id
    private String codigo;
    private String descricao;
}