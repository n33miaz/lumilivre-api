package br.com.lumilivre.api.dto.student;

import java.time.LocalDate;

import br.com.lumilivre.api.dto.common.LocalizedEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentSummaryResponse {

    private String registrationNumber;
    private String fullName;
    private String courseName;
    private LocalDate birthDate;
    private String email;
    private String phoneNumber;
    private LocalizedEnum penaltyCode;
}
