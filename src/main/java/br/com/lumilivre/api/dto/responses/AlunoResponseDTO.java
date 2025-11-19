package br.com.lumilivre.api.dto.responses;

import br.com.lumilivre.api.enums.Penalidade;
import br.com.lumilivre.api.model.AlunoModel;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class AlunoResponseDTO {

    private String matricula;
    private String nomeCompleto;
    private String email;
    private String celular;
    private String cpf;

    @JsonFormat(pattern = "dd/MM/yyyy")
    private LocalDate dataNascimento;

    private String cursoNome;
    private String turnoNome;
    private String moduloNome;

    // Endereço
    private String cep;
    private String logradouro;
    private String bairro;
    private String localidade;
    private String uf;
    private Integer numeroCasa;
    private String complemento;

    // Status
    private Penalidade penalidade;
    private Integer emprestimosCount;

    public AlunoResponseDTO(AlunoModel aluno) {
        this.matricula = aluno.getMatricula();
        this.nomeCompleto = aluno.getNomeCompleto();
        this.email = aluno.getEmail();
        this.celular = aluno.getCelular();
        this.cpf = aluno.getCpf();
        this.dataNascimento = aluno.getDataNascimento();

        this.cursoNome = (aluno.getCurso() != null) ? aluno.getCurso().getNome() : null;
        this.turnoNome = (aluno.getTurno() != null) ? aluno.getTurno().getNome() : null;
        this.moduloNome = (aluno.getModulo() != null) ? aluno.getModulo().getNome() : null;

        this.cep = aluno.getCep();
        this.logradouro = aluno.getLogradouro();
        this.bairro = aluno.getBairro();
        this.localidade = aluno.getLocalidade();
        this.uf = aluno.getUf();
        this.numeroCasa = aluno.getNumero_casa();
        this.complemento = aluno.getComplemento();

        this.penalidade = aluno.getPenalidade();
        this.emprestimosCount = aluno.getEmprestimosCount();
    }
}