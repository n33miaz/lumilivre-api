package br.com.lumilivre.api.dto.reader;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import br.com.lumilivre.api.dto.common.LocalizedEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReaderResponse {

    private String registrationNumber;
    private String fullName;
    private String avatarUrl;
    private String email;
    private String phoneNumber;
    private String cpf;
    private LocalDate birthDate;
    private String courseName;
    private String studyShiftName;
    private String academicModuleName;
    private String readerCategory;
    private String postalCode;
    private String street;
    private String district;
    private String city;
    private String stateCode;
    private Integer streetNumber;
    private String addressComplement;
    private LocalizedEnum penaltyCode;
    private OffsetDateTime penaltyExpiresAt;
    private OffsetDateTime createdAt;
}
