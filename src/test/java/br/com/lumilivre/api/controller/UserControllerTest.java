package br.com.lumilivre.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.lumilivre.api.config.I18nConfig;
import br.com.lumilivre.api.config.MessageResolver;
import br.com.lumilivre.api.enums.Role;
import br.com.lumilivre.api.mapper.UserMapper;
import br.com.lumilivre.api.model.AppUser;
import br.com.lumilivre.api.security.CustomUserDetailsService;
import br.com.lumilivre.api.security.JwtUtil;
import br.com.lumilivre.api.security.StudentAuthorizationService;
import br.com.lumilivre.api.service.AppUserService;
import br.com.lumilivre.api.service.EnumLabelResolver;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

@WebMvcTest(controllers = UserController.class)
@Import({I18nConfig.class, MessageResolver.class, UserMapper.class, EnumLabelResolver.class})
@WithMockUser(roles = "ADMIN")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AppUserService userService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private StudentAuthorizationService studentAuthorizationService;

    @Test
    void listReturnsPtBRContentLanguage() throws Exception {
        AppUser user = AppUser.builder()
                .id(UUID.randomUUID())
                .email("admin@test.com")
                .role(Role.ADMIN)
                .build();
        when(userService.listForAdmin(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(user)));

        mockMvc.perform(get("/api/users").header("Accept-Language", "pt-BR"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Language", "pt-BR"))
                .andExpect(jsonPath("$.content[0].email").value("admin@test.com"));
    }

    @Test
    void listReturnsEnUSContentLanguage() throws Exception {
        AppUser user = AppUser.builder()
                .id(UUID.randomUUID())
                .email("user@test.com")
                .role(Role.LIBRARIAN)
                .build();
        when(userService.listForAdmin(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(user)));

        mockMvc.perform(get("/api/users").header("Accept-Language", "en-US"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Language", "en-US"))
                .andExpect(jsonPath("$.content[0].role.code").value("LIBRARIAN"));
    }
}
