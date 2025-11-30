package br.com.lumilivre.api.dto.aluno;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlunoRequest {

    @Pattern(regexp = "\\d{5}", message = "A matrícula deve conter exatamente 5 dígitos numéricos")
    private String matricula;

    @NotBlank(message = "O nome completo é obrigatório")
    @Size(min = 3, max = 110, message = "O nome deve ter entre 3 e 110 caracteres")
    private String nomeCompleto;

    private String cpf;

    @JsonProperty("data_nascimento")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy")
    private LocalDate dataNascimento;

    private String celular;

    private String email;

    @NotNull(message = "O curso é obrigatório")
    @JsonProperty("curso_id")
    private Integer cursoId;

    @NotNull(message = "O turno é obrigatório")
    @JsonProperty("turno_id")
    private Integer turnoId;

    @NotNull(message = "O módulo é obrigatório")
    @JsonProperty("modulo_id")
    private Integer moduloId;

    @Size(min = 8, max = 8, message = "O CEP deve ter exatamente 8 caracteres (apenas números)")
    private String cep;

    private String logradouro;
    private String complemento;
    private String localidade;
    private String bairro;
    private String uf;

    @JsonProperty("numero_casa")
    private Integer numeroCasa;
}