package br.com.lumilivre.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import br.com.lumilivre.api.config.I18nConfig;
import br.com.lumilivre.api.config.MessageResolver;
import br.com.lumilivre.api.config.MethodSecuritySliceConfig;
import br.com.lumilivre.api.enums.AudienceScope;
import br.com.lumilivre.api.enums.ContentType;
import br.com.lumilivre.api.exception.custom.ResourceNotFoundException;
import br.com.lumilivre.api.mapper.ContentMapper;
import br.com.lumilivre.api.model.AppContent;
import br.com.lumilivre.api.model.Course;
import br.com.lumilivre.api.security.CustomUserDetailsService;
import br.com.lumilivre.api.security.JwtUtil;
import br.com.lumilivre.api.service.AppContentService;
import br.com.lumilivre.api.service.EnumLabelResolver;

/**
 * O contrato do mural: quem chega em cada rota, o que sai no corpo e o que
 * nunca sai. A regra de audiência em si vive no {@code AppContentServiceTest};
 * aqui trava-se a porta e a forma da resposta.
 */
@WebMvcTest(controllers = ContentController.class)
@Import({MethodSecuritySliceConfig.class, I18nConfig.class, MessageResolver.class,
        ContentMapper.class, EnumLabelResolver.class})
class ContentControllerTest {

    private static final UUID CONTENT_ID = UUID.fromString("00000000-0000-4000-8000-000000009001");

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AppContentService contentService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    @WithMockUser(roles = "LIBRARIAN")
    void aListagemDoPainelSaiLocalizada() throws Exception {
        when(contentService.listForAdmin(null, null)).thenReturn(List.of(comunicado()));

        mockMvc.perform(get("/api/contents").header("Accept-Language", "pt-BR"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Language", "pt-BR"))
                .andExpect(jsonPath("$[0].title").value("Biblioteca fechada na sexta"))
                .andExpect(jsonPath("$[0].contentType.code").value("ANNOUNCEMENT"))
                .andExpect(jsonPath("$[0].contentType.label").value("Comunicado"))
                .andExpect(jsonPath("$[0].status.code").value("PUBLISHED"))
                .andExpect(jsonPath("$[0].status.label").value("Publicado"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void oBadgeDeStatusEDerivadoDaJanelaENaoDeUmCampoGuardado() throws Exception {
        AppContent agendado = comunicado();
        agendado.setPublishStartAt(OffsetDateTime.now().plusDays(2));
        when(contentService.listForAdmin(null, null)).thenReturn(List.of(agendado));

        mockMvc.perform(get("/api/contents").header("Accept-Language", "en-US"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status.code").value("SCHEDULED"))
                .andExpect(jsonPath("$[0].status.label").value("Scheduled"));
    }

    @Test
    @WithMockUser(roles = "LIBRARIAN")
    void oFiltroAvancadoRepassaOsQuatroCamposComoVieram() throws Exception {
        when(contentService.searchAdvanced(any(), any(), any(), any())).thenReturn(List.of());

        mockMvc.perform(get("/api/contents/search")
                        .param("type", "WORK")
                        .param("scope", "COURSE")
                        .param("courseId", "7")
                        .param("year", "2024"))
                .andExpect(status().isOk());

        verify(contentService).searchAdvanced(ContentType.WORK, AudienceScope.COURSE, 7, "2024");
    }

    /**
     * O mural do aluno não conta <b>por que</b> ele está vendo aquilo: sem
     * escopo de audiência, sem curso/módulo/turno alvo, sem a janela de
     * publicação. Se um desses campos entrar no DTO do feed, este teste quebra —
     * e é essa a intenção: a segmentação é decisão da escola, não informação do
     * aluno sobre os colegas.
     */
    @Test
    @WithMockUser(roles = "READER")
    void oFeedNaoRevelaAComoOAlunoFoiSegmentado() throws Exception {
        AppContent segmentado = comunicado();
        segmentado.setAudienceScope(AudienceScope.COURSE);
        segmentado.setCourse(curso());
        segmentado.setPublishStartAt(OffsetDateTime.now().minusDays(1));
        segmentado.setPublishEndAt(OffsetDateTime.now().plusDays(1));
        when(contentService.feedForCurrentReader()).thenReturn(List.of(segmentado));

        String body = mockMvc.perform(get("/api/contents/feed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Biblioteca fechada na sexta"))
                .andExpect(jsonPath("$[0].contentType").value("ANNOUNCEMENT"))
                .andReturn().getResponse().getContentAsString();

        List<String> campos = new ArrayList<>();
        new com.fasterxml.jackson.databind.ObjectMapper().readTree(body).get(0).fieldNames()
                .forEachRemaining(campos::add);
        org.assertj.core.api.Assertions.assertThat(campos).doesNotContain(
                "audienceScope", "courseId", "courseName", "academicModuleId",
                "studyShiftId", "publishStartAt", "publishEndAt", "published", "displayOrder");
    }

    @Test
    @WithMockUser(roles = "LIBRARIAN")
    void criarDevolve201ComOCorpoDoConteudo() throws Exception {
        when(contentService.create(any(), any(), any())).thenReturn(comunicado());

        mockMvc.perform(multipart("/api/contents")
                        .file(dados("{\"contentType\":\"ANNOUNCEMENT\",\"title\":\"Biblioteca fechada na sexta\"}"))
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(CONTENT_ID.toString()));
    }

    @Test
    @WithMockUser(roles = "LIBRARIAN")
    void removerDevolve204SemCorpo() throws Exception {
        mockMvc.perform(delete("/api/contents/{id}", CONTENT_ID).with(csrf()))
                .andExpect(status().isNoContent());

        verify(contentService).delete(CONTENT_ID);
    }

    @Test
    @WithMockUser(roles = "READER")
    void conteudoInexistenteVira404Localizado() throws Exception {
        when(contentService.getById(CONTENT_ID))
                .thenThrow(ResourceNotFoundException.ofKey("content.not-found"));

        mockMvc.perform(get("/api/contents/{id}", CONTENT_ID).header("Accept-Language", "en-US"))
                .andExpect(status().isNotFound())
                .andExpect(header().string("Content-Language", "en-US"))
                .andExpect(jsonPath("$.message").value("Content not found."));
    }

    /**
     * A parte {@code data} do multipart é JSON escrito à mão pelo cliente. Um
     * JSON torto é erro dele (400), mas a mensagem do Jackson nomeia classe,
     * campo e posição — detalhe interno que fica no log, não na resposta.
     */
    @Test
    @WithMockUser(roles = "LIBRARIAN")
    void jsonTortoNaParteDataVira400SemVazarOInternoDoJackson() throws Exception {
        String corpo = mockMvc.perform(multipart("/api/contents")
                        .file(dados("{isto-nao-e-json"))
                        .with(csrf())
                        .header("Accept-Language", "pt-BR"))
                .andExpect(status().isBadRequest())
                .andReturn().getResponse().getContentAsString();

        org.assertj.core.api.Assertions.assertThat(corpo)
                .doesNotContain("ContentRequest", "com.fasterxml", "JsonParseException");
        verifyNoInteractions(contentService);
    }

    // ---- barreiras de papel --------------------------------------------------

    /**
     * Publicar comunicado é da equipe. Um leitor que criasse conteúdo estaria
     * escrevendo no mural da escola inteira — e com a audiência que escolhesse.
     */
    @Test
    @WithMockUser(roles = "READER")
    void oLeitorNaoEscreveNoMural() throws Exception {
        mockMvc.perform(multipart("/api/contents")
                        .file(dados("{\"contentType\":\"ANNOUNCEMENT\",\"title\":\"Falso\"}"))
                        .with(csrf()))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/contents/{id}", CONTENT_ID).with(csrf()))
                .andExpect(status().isForbidden());

        verify(contentService, never()).create(any(), any(), any());
        verify(contentService, never()).delete(any());
    }

    /**
     * A listagem do painel devolve rascunho e agendado — conteúdo que ainda não
     * é público. O leitor tem o feed; a listagem não é dele.
     */
    @Test
    @WithMockUser(roles = "READER")
    void oLeitorNaoAlcancaAListagemDoPainel() throws Exception {
        mockMvc.perform(get("/api/contents")).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/contents/search")).andExpect(status().isForbidden());

        verify(contentService, never()).listForAdmin(any(), any());
        verify(contentService, never()).searchAdvanced(any(), any(), any(), any());
    }

    @Test
    @WithMockUser(roles = "READER")
    void oLeitorContinuaComOFeedEComAFichaDoConteudo() throws Exception {
        when(contentService.feedForCurrentReader()).thenReturn(List.of());
        when(contentService.getById(eq(CONTENT_ID))).thenReturn(comunicado());

        mockMvc.perform(get("/api/contents/feed")).andExpect(status().isOk());
        mockMvc.perform(get("/api/contents/{id}", CONTENT_ID)).andExpect(status().isOk());
    }

    // ---- fixtures ------------------------------------------------------------

    private static MockMultipartFile dados(String json) {
        return new MockMultipartFile("data", "", "application/json", json.getBytes());
    }

    private static AppContent comunicado() {
        return AppContent.builder()
                .id(CONTENT_ID)
                .contentType(ContentType.ANNOUNCEMENT)
                .title("Biblioteca fechada na sexta")
                .body("Fechamos para inventario do acervo.")
                .published(true)
                .pinned(false)
                .displayOrder(0)
                .audienceScope(AudienceScope.ALL)
                .createdAt(OffsetDateTime.parse("2026-03-01T10:00:00Z"))
                .updatedAt(OffsetDateTime.parse("2026-03-01T10:00:00Z"))
                .build();
    }

    private static Course curso() {
        Course course = new Course();
        course.setId(7);
        course.setName("Administracao");
        return course;
    }
}
