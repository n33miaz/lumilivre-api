package br.com.lumilivre.api.mapper;

import java.util.Locale;

import br.com.lumilivre.api.dto.common.LocalizedEnum;
import br.com.lumilivre.api.dto.loan.ActiveLoanItem;
import br.com.lumilivre.api.dto.loan.ActiveLoanResponse;
import br.com.lumilivre.api.dto.loan.LoanListItem;
import br.com.lumilivre.api.dto.loan.LoanResponse;
import br.com.lumilivre.api.model.Loan;
import br.com.lumilivre.api.service.EnumLabelResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LoanMapper {

    private final EnumLabelResolver enumLabels;

    public LoanResponse toResponse(Loan loan, Locale locale) {
        LocalizedEnum status = loan.getStatus() != null
                ? LocalizedEnum.of(loan.getStatus(), enumLabels.resolve(loan.getStatus(), locale))
                : null;
        LocalizedEnum penalty = loan.getPenaltyCode() != null
                ? LocalizedEnum.of(loan.getPenaltyCode(), enumLabels.resolve(loan.getPenaltyCode(), locale))
                : null;

        LoanResponse.LoanResponseBuilder b = LoanResponse.builder()
                .id(loan.getId())
                .borrowedAt(loan.getBorrowedAt())
                .dueAt(loan.getDueAt())
                .returnedAt(loan.getReturnedAt())
                .status(status)
                .penaltyCode(penalty)
                .renewalCount(loan.getRenewalCount());

        if (loan.getBookCopy() != null) {
            b.copyCode(loan.getBookCopy().getCopyCode());
            if (loan.getBookCopy().getBook() != null) {
                b.bookId(loan.getBookCopy().getBook().getId())
                        .bookTitle(loan.getBookCopy().getBook().getTitle())
                        .coverUrl(loan.getBookCopy().getBook().getCoverUrl());
            }
        }
        if (loan.getStudent() != null) {
            b.studentName(loan.getStudent().getFullName())
                    .studentRegistrationNumber(loan.getStudent().getRegistrationNumber())
                    .courseName(loan.getStudent().getCourse() != null ? loan.getStudent().getCourse().getName() : null);
        }
        return b.build();
    }

    public LoanResponse fromListItem(LoanListItem item, Locale locale) {
        LocalizedEnum status = item.status() != null
                ? LocalizedEnum.of(item.status(), enumLabels.resolve(item.status(), locale))
                : null;

        return LoanResponse.builder()
                .id(item.id())
                .borrowedAt(item.borrowedAt())
                .dueAt(item.dueAt())
                .status(status)
                .bookTitle(item.bookTitle())
                .copyCode(item.copyCode())
                .studentName(item.studentName())
                .studentRegistrationNumber(item.studentRegistrationNumber())
                .courseName(item.courseName())
                .build();
    }

    public ActiveLoanResponse toActiveResponse(ActiveLoanItem item, Locale locale) {
        LocalizedEnum status = item.status() != null
                ? LocalizedEnum.of(item.status(), enumLabels.resolve(item.status(), locale))
                : null;

        return new ActiveLoanResponse(
                item.id(),
                item.bookTitle(),
                item.studentName(),
                item.studentRegistrationNumber(),
                item.copyCode(),
                item.borrowedAt(),
                item.dueAt(),
                status);
    }
}
