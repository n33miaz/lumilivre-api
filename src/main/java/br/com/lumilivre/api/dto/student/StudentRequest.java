package br.com.lumilivre.api.dto.student;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentRequest {

    @Pattern(regexp = "\\d{5}", message = "{student.registration-number.pattern}")
    private String registrationNumber;

    @NotBlank(message = "{student.form.error.full-name.required}")
    @Size(min = 3, max = 110)
    private String fullName;

    private String cpf;
    private LocalDate birthDate;
    private String phoneNumber;

    @jakarta.validation.constraints.Email(message = "{student.form.error.email.invalid}")
    private String email;

    @NotNull
    private Integer courseId;

    @NotNull
    private Integer studyShiftId;

    @NotNull
    private Integer academicModuleId;

    @Size(min = 8, max = 8)
    private String postalCode;

    private String street;
    private String addressComplement;
    private String city;
    private String district;
    private String stateCode;
    private Integer streetNumber;
    private String penaltyCode;
}
