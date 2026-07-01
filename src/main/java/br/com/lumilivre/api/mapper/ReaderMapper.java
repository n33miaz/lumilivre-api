package br.com.lumilivre.api.mapper;

import java.util.Locale;

import br.com.lumilivre.api.dto.common.LocalizedEnum;
import br.com.lumilivre.api.dto.reader.ReaderListItem;
import br.com.lumilivre.api.dto.reader.ReaderRankingItem;
import br.com.lumilivre.api.dto.reader.ReaderRankingResponse;
import br.com.lumilivre.api.dto.reader.ReaderResponse;
import br.com.lumilivre.api.dto.reader.ReaderSummaryResponse;
import br.com.lumilivre.api.model.Reader;
import br.com.lumilivre.api.service.EnumLabelResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReaderMapper {

    private final EnumLabelResolver enumLabels;

    public ReaderResponse toResponse(Reader s, Locale locale) {
        LocalizedEnum penalty = s.getPenaltyCode() != null
                ? LocalizedEnum.of(s.getPenaltyCode(), enumLabels.resolve(s.getPenaltyCode(), locale))
                : null;

        return ReaderResponse.builder()
                .registrationNumber(s.getRegistrationNumber())
                .fullName(s.getFullName())
                .avatarUrl(s.getAvatarUrl())
                .email(s.getEmail())
                .phoneNumber(s.getPhoneNumber())
                .cpf(s.getCpf())
                .birthDate(s.getBirthDate())
                .courseName(s.getCourse() != null ? s.getCourse().getName() : null)
                .studyShiftName(s.getStudyShift() != null ? s.getStudyShift().getName() : null)
                .academicModuleName(s.getAcademicModule() != null ? s.getAcademicModule().getName() : null)
                .readerCategory(s.getReaderCategory())
                .postalCode(s.getPostalCode())
                .street(s.getStreet())
                .district(s.getDistrict())
                .city(s.getCity())
                .stateCode(s.getStateCode())
                .streetNumber(s.getStreetNumber())
                .addressComplement(s.getAddressComplement())
                .penaltyCode(penalty)
                .penaltyExpiresAt(s.getPenaltyExpiresAt())
                .createdAt(s.getCreatedAt())
                .build();
    }

    public ReaderSummaryResponse toSummary(ReaderListItem item, Locale locale) {
        LocalizedEnum penalty = item.penaltyCode() != null
                ? LocalizedEnum.of(item.penaltyCode(), enumLabels.resolve(item.penaltyCode(), locale))
                : null;

        return ReaderSummaryResponse.builder()
                .registrationNumber(item.registrationNumber())
                .fullName(item.fullName())
                .courseName(item.courseName())
                .readerCategory(item.readerCategory())
                .birthDate(item.birthDate())
                .email(item.email())
                .phoneNumber(item.phoneNumber())
                .penaltyCode(penalty)
                .build();
    }

    public ReaderRankingResponse toRanking(ReaderRankingItem item) {
        return new ReaderRankingResponse(item.registrationNumber(), item.fullName(), item.loanCount());
    }
}
