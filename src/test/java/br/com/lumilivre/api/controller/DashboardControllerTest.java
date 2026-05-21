package br.com.lumilivre.api.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import br.com.lumilivre.api.config.I18nConfig;
import br.com.lumilivre.api.config.MessageResolver;
import br.com.lumilivre.api.dto.dashboard.DashboardStatsResponse;
import br.com.lumilivre.api.security.CustomUserDetailsService;
import br.com.lumilivre.api.security.JwtUtil;
import br.com.lumilivre.api.security.StudentAuthorizationService;
import br.com.lumilivre.api.service.DashboardService;
import br.com.lumilivre.api.service.EnumLabelResolver;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = DashboardController.class)
@Import({I18nConfig.class, MessageResolver.class, EnumLabelResolver.class})
@WithMockUser(roles = "ADMIN")
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DashboardService dashboardService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private StudentAuthorizationService studentAuthorizationService;

    @Test
    void statsReturnsPtBRContentLanguage() throws Exception {
        when(dashboardService.getStats())
                .thenReturn(new DashboardStatsResponse(5, 1, 20, 3.5, 2, 4));

        mockMvc.perform(get("/api/dashboard/stats").header("Accept-Language", "pt-BR"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Language", "pt-BR"))
                .andExpect(jsonPath("$.activeLoans").value(5));
    }

    @Test
    void statsReturnsEnUSContentLanguage() throws Exception {
        when(dashboardService.getStats())
                .thenReturn(new DashboardStatsResponse(3, 0, 10, 2.0, 1, 0));

        mockMvc.perform(get("/api/dashboard/stats").header("Accept-Language", "en-US"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Language", "en-US"))
                .andExpect(jsonPath("$.overdueLoans").value(0));
    }

    @Test
    void topBooksReturnsList() throws Exception {
        when(dashboardService.getTopBooks()).thenReturn(List.of());

        mockMvc.perform(get("/api/dashboard/top-books").header("Accept-Language", "pt-BR"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Language", "pt-BR"))
                .andExpect(jsonPath("$").isArray());
    }
}
