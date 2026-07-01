package br.com.lumilivre.api.dto.auth;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    private UUID id;
    private String email;
    private String role;
    private String readerRegistrationNumber;
    private String token;
    private boolean initialPasswordChange;
}
