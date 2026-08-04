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

    // Obrigatória só na criação (validada no service via user.password.required);
    // em branco na edição significa "manter a senha atual".
    private String password;

    private String readerRegistrationNumber;
    private Role role;
}
