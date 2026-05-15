package br.com.lumilivre.api.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChangePasswordRequest {

    private String registrationNumber;

    @NotBlank
    private String currentPassword;

    @NotBlank
    private String newPassword;
}
