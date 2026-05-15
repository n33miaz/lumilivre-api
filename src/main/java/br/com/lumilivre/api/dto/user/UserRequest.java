package br.com.lumilivre.api.dto.user;

import br.com.lumilivre.api.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRequest {

    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String password;

    private String studentRegistrationNumber;
    private Role role;
}
