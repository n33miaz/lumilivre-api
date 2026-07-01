package br.com.lumilivre.api.mapper;

import java.util.Locale;

import br.com.lumilivre.api.dto.common.LocalizedEnum;
import br.com.lumilivre.api.dto.user.UserResponse;
import br.com.lumilivre.api.dto.user.UserSummaryResponse;
import br.com.lumilivre.api.model.AppUser;
import br.com.lumilivre.api.service.EnumLabelResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserMapper {

    private final EnumLabelResolver enumLabels;

    public UserResponse toResponse(AppUser user, Locale locale) {
        LocalizedEnum role = user.getRole() != null
                ? LocalizedEnum.of(user.getRole(), enumLabels.resolve(user.getRole(), locale))
                : null;
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .role(role)
                .readerRegistrationNumber(
                        user.getReader() != null ? user.getReader().getRegistrationNumber() : null)
                .build();
    }

    public UserSummaryResponse toSummary(AppUser user, Locale locale) {
        LocalizedEnum role = user.getRole() != null
                ? LocalizedEnum.of(user.getRole(), enumLabels.resolve(user.getRole(), locale))
                : null;
        return UserSummaryResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .role(role)
                .build();
    }
}
