package br.com.lumilivre.api.controller.v2;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import br.com.lumilivre.api.config.I18nConfig;
import br.com.lumilivre.api.config.MessageResolver;
import br.com.lumilivre.api.dto.student.StudentListItem;
import br.com.lumilivre.api.mapper.v2.StudentMapper;
import br.com.lumilivre.api.model.Student;
import br.com.lumilivre.api.security.CustomUserDetailsService;
import br.com.lumilivre.api.security.JwtUtil;
import br.com.lumilivre.api.security.StudentAuthorizationService;
import br.com.lumilivre.api.service.EnumLabelResolver;
import br.com.lumilivre.api.service.StudentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = StudentController.class)
@Import({I18nConfig.class, MessageResolver.class, StudentMapper.class, EnumLabelResolver.class})
@WithMockUser(roles = "ADMIN")
class StudentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StudentService studentService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private StudentAuthorizationService studentAuthorizationService;

    @Test
    void listReturnsPtBRByDefault() throws Exception {
        StudentListItem item = new StudentListItem(null, "12345", "Admin", "Joao Silva", null, null, null);
        when(studentService.listarParaAdminV2(any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(item)));

        mockMvc.perform(get("/api/v2/students").header("Accept-Language", "pt-BR"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Language", "pt-BR"))
                .andExpect(jsonPath("$.content[0].registrationNumber").value("12345"))
                .andExpect(jsonPath("$.content[0].fullName").value("Joao Silva"));
    }

    @Test
    void listReturnsEnUS() throws Exception {
        StudentListItem item = new StudentListItem(null, "12345", "Admin", "John Doe", null, null, null);
        when(studentService.listarParaAdminV2(any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(item)));

        mockMvc.perform(get("/api/v2/students").header("Accept-Language", "en-US"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Language", "en-US"))
                .andExpect(jsonPath("$.content[0].registrationNumber").value("12345"));
    }

    @Test
    void getOneSetsContentLanguageFromAcceptHeader() throws Exception {
        Student student = new Student();
        student.setRegistrationNumber("12345");
        student.setFullName("Maria");
        when(studentService.buscarPorMatricula(anyString())).thenReturn(student);

        mockMvc.perform(get("/api/v2/students/12345").header("Accept-Language", "en-US"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Language", "en-US"))
                .andExpect(jsonPath("$.registrationNumber").value("12345"))
                .andExpect(jsonPath("$.fullName").value("Maria"));
    }
}
