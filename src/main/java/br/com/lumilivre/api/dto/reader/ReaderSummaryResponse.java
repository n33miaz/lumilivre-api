package br.com.lumilivre.api.dto.reader;

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
public class ReaderSummaryResponse {

    private String registrationNumber;
    private String fullName;
    private String courseName;
    private String readerCategory;
    private LocalDate birthDate;
    private String email;
    private String phoneNumber;
    private LocalizedEnum penaltyCode;
}
