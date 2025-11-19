package br.com.lumilivre.api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlunoDTO {

    private String matricula;
    private String nomeCompleto;
    private String cpf;

    @JsonProperty("data_nascimento")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy")
    private LocalDate dataNascimento;

    private String celular;
    private String email;

    @JsonProperty("curso_id")
    private Integer cursoId;

    @JsonProperty("turno_id")
    private Integer turnoId;

    @JsonProperty("modulo_id")
    private Integer moduloId;

    private String cep;
    private String logradouro;
    private String complemento;
    private String localidade;
    private String bairro;
    private String uf;

    @JsonProperty("numero_casa")
    private Integer numeroCasa;
}