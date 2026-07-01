package br.com.lumilivre.api.dto.reader;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
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
public class ReaderRequest {

    @Pattern(regexp = "\\d{5}", message = "{reader.registration-number.pattern}")
    private String registrationNumber;

    @NotBlank(message = "{reader.form.error.full-name.required}")
    @Size(min = 3, max = 110)
    private String fullName;

    private String cpf;
    private LocalDate birthDate;
    private String phoneNumber;

    @jakarta.validation.constraints.Email(message = "{reader.form.error.email.invalid}")
    private String email;

    private Integer courseId;

    private Integer studyShiftId;

    private Integer academicModuleId;

    @Size(max = 80)
    private String readerCategory;

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
