package br.com.lumilivre.api.dto.reservation;

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
public class ReservationResponse {

    private UUID id;
    private String studentRegistrationNumber;
    private String studentName;
    private UUID bookId;
    private String bookTitle;
    private LocalizedEnum status;
    private Integer queuePosition;
    private OffsetDateTime expiresAt;
    private OffsetDateTime createdAt;
}
