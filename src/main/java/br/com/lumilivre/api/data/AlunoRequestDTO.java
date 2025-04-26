package br.com.lumilivre.api.data;

import lombok.Data;

import java.time.LocalDate;

@Data
public class AlunoRequestDTO {

    private String matricula;
    private String nome;
    private String sobrenome;
    private String cpf;
    private LocalDate dataNascimento;
    private String celular;
    private String email;
    private Long cursoId;
    private String cep; // para buscar o endereço
}
