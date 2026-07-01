package br.com.lumilivre.api.mapper;

import java.util.Locale;

import br.com.lumilivre.api.dto.common.LocalizedEnum;
import br.com.lumilivre.api.dto.reservation.ReservationResponse;
import br.com.lumilivre.api.model.Reservation;
import br.com.lumilivre.api.service.EnumLabelResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReservationMapper {

    private final EnumLabelResolver enumLabels;

    public ReservationResponse toResponse(Reservation r, Locale locale) {
        LocalizedEnum status = r.getStatus() != null
                ? LocalizedEnum.of(r.getStatus(), enumLabels.resolve(r.getStatus(), locale))
                : null;

        return ReservationResponse.builder()
                .id(r.getId())
                .readerRegistrationNumber(r.getReader() != null ? r.getReader().getRegistrationNumber() : null)
                .readerName(r.getReader() != null ? r.getReader().getFullName() : null)
                .bookId(r.getBook() != null ? r.getBook().getId() : null)
                .bookTitle(r.getBook() != null ? r.getBook().getTitle() : null)
                .status(status)
                .queuePosition(r.getQueuePosition())
                .expiresAt(r.getExpiresAt())
                .createdAt(r.getCreatedAt())
                .build();
    }
}
