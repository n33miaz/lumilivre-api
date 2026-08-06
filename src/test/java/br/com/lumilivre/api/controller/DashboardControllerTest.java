package br.com.lumilivre.api.controller;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import br.com.lumilivre.api.config.I18nConfig;
import br.com.lumilivre.api.config.MessageResolver;
import br.com.lumilivre.api.config.MethodSecuritySliceConfig;
import br.com.lumilivre.api.dto.dashboard.DashboardStatsResponse;
import br.com.lumilivre.api.security.CustomUserDetailsService;
import br.com.lumilivre.api.security.JwtUtil;
import br.com.lumilivre.api.security.ReaderAuthorizationService;
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
@Import({MethodSecuritySliceConfig.class, I18nConfig.class, MessageResolver.class, EnumLabelResolver.class})
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

    @MockBean(name = "readerAuthz")
    private ReaderAuthorizationService readerAuthorizationService;

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

    /**
     * O painel é o agregado da operação: atrasos, multas, o que a biblioteca
     * está deixando de entregar. Serve à gestão e a mais ninguém — o leitor tem
     * o próprio histórico, não o do acervo inteiro.
     */
    @Test
    @WithMockUser(roles = "READER")
    void aReaderCannotOpenTheManagementPanel() throws Exception {
        mockMvc.perform(get("/api/dashboard/stats")).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/dashboard/top-books")).andExpect(status().isForbidden());

        verifyNoInteractions(dashboardService);
    }
}
