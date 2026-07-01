package br.com.lumilivre.api.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.lumilivre.api.config.I18nConfig;
import br.com.lumilivre.api.config.MessageResolver;
import br.com.lumilivre.api.security.CustomUserDetailsService;
import br.com.lumilivre.api.security.JwtUtil;
import br.com.lumilivre.api.service.ReportService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = ReportController.class)
@Import({I18nConfig.class, MessageResolver.class})
@WithMockUser(roles = "ADMIN")
class ReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReportService reportService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void loansReportReturnsPdfContentType() throws Exception {
        mockMvc.perform(get("/api/reports/loans"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=loans-report.pdf"));
    }

    @Test
    void readersReportReturnsPdfContentType() throws Exception {
        mockMvc.perform(get("/api/reports/readers"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=readers-report.pdf"));
    }

    @Test
    void booksReportReturnsPdfContentType() throws Exception {
        mockMvc.perform(get("/api/reports/books"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=books-report.pdf"));
    }

    @Test
    void booksStatisticsReportReturnsPdfContentType() throws Exception {
        mockMvc.perform(get("/api/reports/books/statistics"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"));
    }

    @Test
    void copiesReportReturnsPdfContentType() throws Exception {
        mockMvc.perform(get("/api/reports/copies"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=copies-report.pdf"));
    }

    @Test
    @WithMockUser(roles = "LIBRARIAN")
    void librarianCanAccessReports() throws Exception {
        mockMvc.perform(get("/api/reports/loans"))
                .andExpect(status().isOk());
    }
}
