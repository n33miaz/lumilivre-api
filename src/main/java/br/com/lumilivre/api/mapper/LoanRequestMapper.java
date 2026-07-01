package br.com.lumilivre.api.mapper;

import java.util.Locale;

import br.com.lumilivre.api.dto.common.LocalizedEnum;
import br.com.lumilivre.api.dto.loanrequest.LoanRequestResponse;
import br.com.lumilivre.api.model.LoanRequest;
import br.com.lumilivre.api.service.EnumLabelResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LoanRequestMapper {

    private final EnumLabelResolver enumLabels;

    public LoanRequestResponse toResponse(LoanRequest request, Locale locale) {
        LocalizedEnum status = request.getStatus() != null
                ? LocalizedEnum.of(request.getStatus(), enumLabels.resolve(request.getStatus(), locale))
                : null;

        return LoanRequestResponse.builder()
                .id(request.getId())
                .readerName(request.getReader().getFullName())
                .readerRegistrationNumber(request.getReader().getRegistrationNumber())
                .copyCode(request.getBookCopy().getCopyCode())
                .bookId(request.getBookCopy().getBook().getId())
                .bookTitle(request.getBookCopy().getBook().getTitle())
                .requestedAt(request.getRequestedAt())
                .status(status)
                .notes(request.getNote())
                .build();
    }
}
