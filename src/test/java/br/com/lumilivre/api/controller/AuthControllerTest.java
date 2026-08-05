package br.com.lumilivre.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import br.com.lumilivre.api.config.I18nConfig;
import br.com.lumilivre.api.config.MessageResolver;
import br.com.lumilivre.api.dto.auth.LoginResponse;
import br.com.lumilivre.api.enums.AccessEvent;
import br.com.lumilivre.api.enums.Role;
import br.com.lumilivre.api.model.AppUser;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import br.com.lumilivre.api.security.CustomUserDetailsService;
import br.com.lumilivre.api.security.JwtUtil;
import br.com.lumilivre.api.security.ReaderAuthorizationService;
import br.com.lumilivre.api.service.AccessLogService;
import br.com.lumilivre.api.service.AppUserService;
import br.com.lumilivre.api.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({I18nConfig.class, MessageResolver.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @MockBean
    private AppUserService userService;

    @MockBean
    private AccessLogService accessLogService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private ReaderAuthorizationService readerAuthorizationService;

    @Test
    void loginReturnsTokenAndContentLanguage() throws Exception {
        LoginResponse response = LoginResponse.builder()
                .id(UUID.randomUUID())
                .email("user@test.com")
                .role("ADMIN")
                .token("jwt-token-here")
                .initialPasswordChange(false)
                .build();
        when(authService.login(anyString(), anyString())).thenReturn(response);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"user@test.com\",\"password\":\"secret\"}")
                        .header("Accept-Language", "pt-BR"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Language", "pt-BR"))
                .andExpect(jsonPath("$.token").value("jwt-token-here"))
                .andExpect(jsonPath("$.email").value("user@test.com"));
    }

    @Test
    void validateTokenReturnsValidFlag() throws Exception {
        when(authService.validarTokenReset("abc123")).thenReturn(true);

        mockMvc.perform(get("/api/auth/validate-token/abc123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true));
    }

    @Test
    void changePasswordReturnsAFreshTokenBecauseTheOldOneWasRevoked() throws Exception {
        AppUser updated = AppUser.builder()
                .email("user@test.com")
                .role(Role.ADMIN)
                .build();
        when(userService.changePassword(any())).thenReturn(updated);
        when(authService.issueToken(updated)).thenReturn("jwt-after-change");

        mockMvc.perform(put("/api/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"old-pass\",\"newPassword\":\"chuva-de-papel-77\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-after-change"));
    }

    @Test
    void logoutRevokesTokensAndRecordsTheEvent() throws Exception {
        when(authService.logout("admin@test.com")).thenReturn("admin@test.com");

        mockMvc.perform(post("/api/auth/logout").principal(
                        new UsernamePasswordAuthenticationToken("admin@test.com", null,
                                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")))))
                .andExpect(status().isNoContent());

        verify(authService).logout("admin@test.com");
        verify(accessLogService).record(AccessEvent.LOGOUT, "admin@test.com", "ROLE_ADMIN",
                AccessLogService.RESULT_SUCCESS, null);
    }

    @Test
    void loginReturnsEnUSContentLanguage() throws Exception {
        LoginResponse response = LoginResponse.builder()
                .id(UUID.randomUUID())
                .email("user@test.com")
                .role("LIBRARIAN")
                .token("jwt-en")
                .initialPasswordChange(false)
                .build();
        when(authService.login(anyString(), anyString())).thenReturn(response);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"user@test.com\",\"password\":\"secret\"}")
                        .header("Accept-Language", "en-US"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Language", "en-US"));
    }
}
