package br.com.lumilivre.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import br.com.lumilivre.api.config.I18nConfig;
import br.com.lumilivre.api.config.MessageResolver;
import br.com.lumilivre.api.dto.loan.LoanListItem;
import br.com.lumilivre.api.enums.LoanStatus;
import br.com.lumilivre.api.mapper.LoanMapper;
import br.com.lumilivre.api.security.CustomUserDetailsService;
import br.com.lumilivre.api.security.JwtUtil;
import br.com.lumilivre.api.security.StudentAuthorizationService;
import br.com.lumilivre.api.service.EnumLabelResolver;
import br.com.lumilivre.api.service.LoanService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = LoanController.class)
@Import({I18nConfig.class, MessageResolver.class, LoanMapper.class, EnumLabelResolver.class})
@WithMockUser(roles = "ADMIN")
class LoanControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LoanService loanService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private StudentAuthorizationService studentAuthorizationService;

    @Test
    void listReturnsPtBRWithEnFieldNames() throws Exception {
        UUID id = UUID.randomUUID();
        LoanListItem item = new LoanListItem(
                id, LoanStatus.OVERDUE, "Dom Quixote", "T001",
                "Joao Silva", "12345", "Administracao",
                OffsetDateTime.now().minusDays(30), OffsetDateTime.now().minusDays(1), null);
        when(loanService.buscarEmprestimoParaListaAdminV2(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(item)));

        mockMvc.perform(get("/api/loans").header("Accept-Language", "pt-BR"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Language", "pt-BR"))
                .andExpect(jsonPath("$.content[0].id").value(id.toString()))
                .andExpect(jsonPath("$.content[0].bookTitle").value("Dom Quixote"))
                .andExpect(jsonPath("$.content[0].studentRegistrationNumber").value("12345"))
                .andExpect(jsonPath("$.content[0].status.code").value("OVERDUE"))
                .andExpect(jsonPath("$.content[0].status.label").value("Atrasado"));
    }

    @Test
    void listReturnsEnUSLabels() throws Exception {
        UUID id = UUID.randomUUID();
        LoanListItem item = new LoanListItem(
                id, LoanStatus.ACTIVE, "Dom Quixote", "T001",
                "Joao Silva", "12345", "Administracao",
                OffsetDateTime.now().minusDays(5), OffsetDateTime.now().plusDays(10), null);
        when(loanService.buscarEmprestimoParaListaAdminV2(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(item)));

        mockMvc.perform(get("/api/loans").header("Accept-Language", "en-US"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Language", "en-US"))
                .andExpect(jsonPath("$.content[0].status.code").value("ACTIVE"))
                .andExpect(jsonPath("$.content[0].status.label").value("Active"));
    }
}
