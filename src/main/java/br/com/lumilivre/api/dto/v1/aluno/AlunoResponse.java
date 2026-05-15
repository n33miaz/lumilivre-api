package br.com.lumilivre.api.dto.v1.aluno;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;

import br.com.lumilivre.api.enums.PenaltyCode;
import br.com.lumilivre.api.model.Student;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlunoResponse {

    private String matricula;
    private String nomeCompleto;
    private String foto;
    private String email;
    private String celular;
    private String cpf;

    @JsonFormat(pattern = "dd/MM/yyyy")
    private LocalDate dataNascimento;

    private String cursoNome;
    private String turnoNome;
    private String moduloNome;
    private String cep;
    private String logradouro;
    private String bairro;
    private String localidade;
    private String uf;
    private Integer numeroCasa;
    private String complemento;
    private PenaltyCode penalidade;

    public AlunoResponse(Student aluno) {
        this.matricula = aluno.getRegistrationNumber();
        this.nomeCompleto = aluno.getFullName();
        this.foto = aluno.getAvatarUrl();
        this.email = aluno.getEmail();
        this.celular = aluno.getPhoneNumber();
        this.cpf = aluno.getCpf();
        this.dataNascimento = aluno.getBirthDate();
        this.cursoNome = (aluno.getCourse() != null) ? aluno.getCourse().getName() : null;
        this.turnoNome = (aluno.getStudyShift() != null) ? aluno.getStudyShift().getName() : null;
        this.moduloNome = (aluno.getAcademicModule() != null) ? aluno.getAcademicModule().getName() : null;
        this.cep = aluno.getPostalCode();
        this.logradouro = aluno.getStreet();
        this.bairro = aluno.getDistrict();
        this.localidade = aluno.getCity();
        this.uf = aluno.getStateCode();
        this.numeroCasa = aluno.getStreetNumber();
        this.complemento = aluno.getAddressComplement();
        this.penalidade = aluno.getPenaltyCode();
    }
}
