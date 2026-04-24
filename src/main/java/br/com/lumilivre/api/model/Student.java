package br.com.lumilivre.api.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import br.com.lumilivre.api.enums.Penalidade;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity(name = "Student")
@Table(name = "aluno")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Student {

    @Id
    @Column(name = "matricula", nullable = false, length = 5, unique = true)
    private String registrationNumber;

    @NotNull
    @Column(name = "nome_completo", nullable = false, length = 255)
    private String fullName;

    @Column(name = "foto", length = 500)
    private String avatarUrl;

    @Column(name = "cpf", length = 11)
    private String cpf;

    @JsonFormat(pattern = "dd/MM/yyyy")
    @Column(name = "data_nascimento")
    private LocalDate birthDate;

    @Column(name = "celular", length = 11)
    private String phoneNumber;

    @Column(name = "email", length = 100)
    private String email;

    @ManyToOne
    @JoinColumn(name = "curso_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Course course;

    @ManyToOne
    @JoinColumn(name = "turno_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private StudyShift studyShift;

    @ManyToOne
    @JoinColumn(name = "modulo_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private AcademicModule academicModule;

    @JsonManagedReference
    @OneToOne(mappedBy = "student", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private AppUser appUser;

    @Size(min = 8, max = 8, message = "CEP deve ter exatamente 8 caracteres")
    @Column(name = "cep", length = 8)
    private String postalCode;

    @Column(name = "logradouro", length = 255)
    private String street;

    @Column(name = "complemento", length = 55)
    private String addressComplement;

    @Column(name = "bairro", length = 55)
    private String district;

    @Column(name = "localidade", length = 55)
    private String city;

    @Column(name = "uf", length = 2)
    private String stateCode;

    @Column(name = "numero_casa")
    private Integer streetNumber;

    @Column(name = "penalidade", length = 20)
    private Penalidade penaltyCode;

    @Column(name = "penalidade_expira_em")
    private LocalDateTime penaltyExpiresAt;

    @Builder.Default
    @Column(name = "emprestimos_count", nullable = false)
    private Integer loansCount = 0;

    @Column(name = "data_inclusao", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    public String getMatricula() {
        return registrationNumber;
    }

    public void setMatricula(String matricula) {
        this.registrationNumber = matricula;
    }

    public String getNomeCompleto() {
        return fullName;
    }

    public void setNomeCompleto(String nomeCompleto) {
        this.fullName = nomeCompleto;
    }

    public String getFoto() {
        return avatarUrl;
    }

    public void setFoto(String foto) {
        this.avatarUrl = foto;
    }

    public LocalDate getDataNascimento() {
        return birthDate;
    }

    public void setDataNascimento(LocalDate dataNascimento) {
        this.birthDate = dataNascimento;
    }

    public String getCelular() {
        return phoneNumber;
    }

    public void setCelular(String celular) {
        this.phoneNumber = celular;
    }

    public Course getCurso() {
        return course;
    }

    public void setCurso(Course curso) {
        this.course = curso;
    }

    public StudyShift getTurno() {
        return studyShift;
    }

    public void setTurno(StudyShift turno) {
        this.studyShift = turno;
    }

    public AcademicModule getModulo() {
        return academicModule;
    }

    public void setModulo(AcademicModule modulo) {
        this.academicModule = modulo;
    }

    public AppUser getUsuario() {
        return appUser;
    }

    public void setUsuario(AppUser usuario) {
        this.appUser = usuario;
    }

    public String getCep() {
        return postalCode;
    }

    public void setCep(String cep) {
        this.postalCode = cep;
    }

    public String getLogradouro() {
        return street;
    }

    public void setLogradouro(String logradouro) {
        this.street = logradouro;
    }

    public String getComplemento() {
        return addressComplement;
    }

    public void setComplemento(String complemento) {
        this.addressComplement = complemento;
    }

    public String getBairro() {
        return district;
    }

    public void setBairro(String bairro) {
        this.district = bairro;
    }

    public String getLocalidade() {
        return city;
    }

    public void setLocalidade(String localidade) {
        this.city = localidade;
    }

    public String getUf() {
        return stateCode;
    }

    public void setUf(String uf) {
        this.stateCode = uf;
    }

    public Integer getNumero_casa() {
        return streetNumber;
    }

    public void setNumero_casa(Integer numeroCasa) {
        this.streetNumber = numeroCasa;
    }

    public Penalidade getPenalidade() {
        return penaltyCode;
    }

    public void setPenalidade(Penalidade penalidade) {
        this.penaltyCode = penalidade;
    }

    public LocalDateTime getPenalidadeExpiraEm() {
        return penaltyExpiresAt;
    }

    public void setPenalidadeExpiraEm(LocalDateTime penalidadeExpiraEm) {
        this.penaltyExpiresAt = penalidadeExpiraEm;
    }

    public int getEmprestimosCount() {
        return loansCount != null ? loansCount : 0;
    }

    public void setEmprestimosCount(Integer emprestimosCount) {
        this.loansCount = emprestimosCount;
    }

    public void incrementarEmprestimos() {
        this.loansCount = getEmprestimosCount() + 1;
    }

    public void decrementarEmprestimos() {
        if (getEmprestimosCount() > 0) {
            this.loansCount = getEmprestimosCount() - 1;
        }
    }

    public LocalDateTime getDataInclusao() {
        return createdAt;
    }

    public void setDataInclusao(LocalDateTime dataInclusao) {
        this.createdAt = dataInclusao;
    }
}
