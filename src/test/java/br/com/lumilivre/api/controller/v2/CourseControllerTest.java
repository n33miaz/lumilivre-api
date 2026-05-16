package br.com.lumilivre.api.controller.v2;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import br.com.lumilivre.api.config.I18nConfig;
import br.com.lumilivre.api.config.MessageResolver;
import br.com.lumilivre.api.dto.course.CourseResponse;
import br.com.lumilivre.api.mapper.v2.CourseMapper;
import br.com.lumilivre.api.model.Course;
import br.com.lumilivre.api.security.CustomUserDetailsService;
import br.com.lumilivre.api.security.JwtUtil;
import br.com.lumilivre.api.service.CourseService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = CourseController.class)
@Import({I18nConfig.class, MessageResolver.class})
@WithMockUser(roles = "ADMIN")
class CourseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CourseService courseService;

    @MockBean
    private CourseMapper mapper;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void listReturnsOkWithContentLanguage() throws Exception {
        when(courseService.buscarCursoParaListaAdmin(isNull(), any(Pageable.class)))
                .thenReturn(Page.empty());

        mockMvc.perform(get("/api/v2/courses").header("Accept-Language", "en-US"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Language", "en-US"));
    }

    @Test
    void createReturns201() throws Exception {
        Course entity = new Course();
        entity.setId(1);
        entity.setName("Ciência da Computação");
        when(courseService.cadastrar(any())).thenReturn(entity);
        when(mapper.toResponse(entity)).thenReturn(new CourseResponse(1, "Ciência da Computação"));

        mockMvc.perform(post("/api/v2/courses").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Ciência da Computação\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void statisticsReturnsOkWithContentLanguage() throws Exception {
        when(courseService.buscarEstatisticas()).thenReturn(List.of());

        mockMvc.perform(get("/api/v2/courses/statistics").header("Accept-Language", "pt-BR"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Language", "pt-BR"));
    }

    @Test
    void deleteReturnsNoContent() throws Exception {
        doNothing().when(courseService).excluir(1);

        mockMvc.perform(delete("/api/v2/courses/1").with(csrf()))
                .andExpect(status().isNoContent());
    }
}
