package br.com.lumilivre.api.dto.loanrequest;

import java.time.OffsetDateTime;
import java.util.UUID;

import br.com.lumilivre.api.dto.common.LocalizedEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanRequestResponse {

    private UUID id;
    private String studentName;
    private String studentRegistrationNumber;
    private String copyCode;
    private UUID bookId;
    private String bookTitle;
    private OffsetDateTime requestedAt;
    private LocalizedEnum status;
    private String notes;
}
