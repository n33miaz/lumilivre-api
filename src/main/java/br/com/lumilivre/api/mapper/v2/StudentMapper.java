package br.com.lumilivre.api.mapper.v2;

import java.util.Locale;

import br.com.lumilivre.api.dto.common.LocalizedEnum;
import br.com.lumilivre.api.dto.student.StudentListItem;
import br.com.lumilivre.api.dto.student.StudentRankingItem;
import br.com.lumilivre.api.dto.student.StudentRankingResponse;
import br.com.lumilivre.api.dto.student.StudentResponse;
import br.com.lumilivre.api.dto.student.StudentSummaryResponse;
import br.com.lumilivre.api.model.Student;
import br.com.lumilivre.api.service.EnumLabelResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StudentMapper {

    private final EnumLabelResolver enumLabels;

    public StudentResponse toResponse(Student s, Locale locale) {
        LocalizedEnum penalty = s.getPenaltyCode() != null
                ? LocalizedEnum.of(s.getPenaltyCode(), enumLabels.resolve(s.getPenaltyCode(), locale))
                : null;

        return StudentResponse.builder()
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

    public StudentSummaryResponse toSummary(StudentListItem item, Locale locale) {
        LocalizedEnum penalty = item.penaltyCode() != null
                ? LocalizedEnum.of(item.penaltyCode(), enumLabels.resolve(item.penaltyCode(), locale))
                : null;

        return StudentSummaryResponse.builder()
                .registrationNumber(item.registrationNumber())
                .fullName(item.fullName())
                .courseName(item.courseName())
                .birthDate(item.birthDate())
                .email(item.email())
                .phoneNumber(item.phoneNumber())
                .penaltyCode(penalty)
                .build();
    }

    public StudentRankingResponse toRanking(StudentRankingItem item) {
        return new StudentRankingResponse(item.registrationNumber(), item.fullName(), item.loanCount());
    }
}
