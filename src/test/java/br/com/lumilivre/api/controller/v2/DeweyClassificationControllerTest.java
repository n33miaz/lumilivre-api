package br.com.lumilivre.api.controller.v2;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import br.com.lumilivre.api.config.I18nConfig;
import br.com.lumilivre.api.config.MessageResolver;
import br.com.lumilivre.api.model.DeweyClassification;
import br.com.lumilivre.api.repository.DeweyClassificationRepository;
import br.com.lumilivre.api.security.CustomUserDetailsService;
import br.com.lumilivre.api.security.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = DeweyClassificationController.class)
@Import({I18nConfig.class, MessageResolver.class})
@WithMockUser(roles = "ADMIN")
class DeweyClassificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DeweyClassificationRepository deweyClassificationRepository;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void listReturnsOkWithContentLanguage() throws Exception {
        DeweyClassification cdd = new DeweyClassification();
        cdd.setCode("100");
        cdd.setDescription("Philosophy");
        when(deweyClassificationRepository.findAll()).thenReturn(List.of(cdd));

        mockMvc.perform(get("/api/v2/dewey-classifications").header("Accept-Language", "en-US"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Language", "en-US"))
                .andExpect(jsonPath("$[0].code").value("100"))
                .andExpect(jsonPath("$[0].description").value("Philosophy"));
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void studentCanList() throws Exception {
        when(deweyClassificationRepository.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/api/v2/dewey-classifications"))
                .andExpect(status().isOk());
    }
}
