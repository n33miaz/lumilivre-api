package br.com.lumilivre.api.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

import br.com.lumilivre.api.dto.content.ContentFeedItemResponse;
import br.com.lumilivre.api.dto.content.ContentResponse;
import br.com.lumilivre.api.enums.AudienceScope;
import br.com.lumilivre.api.enums.ContentType;
import br.com.lumilivre.api.model.AcademicModule;
import br.com.lumilivre.api.model.AppContent;
import br.com.lumilivre.api.model.Course;
import br.com.lumilivre.api.model.StudyShift;
import br.com.lumilivre.api.service.EnumLabelResolver;

/**
 * O mapeamento do conteúdo tem duas decisões que valem teste: o badge de status
 * é <b>derivado</b> da janela (não existe coluna para ele, então errar aqui
 * mostra "publicado" para algo que o app não entrega), e o nome do alvo de
 * audiência acompanha o id — é o que a tela do painel exibe em vez de "curso 7".
 */
class ContentMapperTest {

    private static final Locale PT = Locale.forLanguageTag("pt-BR");
    private static final Locale EN = Locale.forLanguageTag("en-US");
    private static final OffsetDateTime CRIADO_EM = OffsetDateTime.parse("2026-03-01T10:00:00Z");

    private final ContentMapper mapper = new ContentMapper(new EnumLabelResolver(bundle()));

    @Test
    void oBadgeDeStatusSegueAJanelaDePublicacao() {
        assertThat(mapper.toResponse(comunicado(), PT).getStatus().code()).isEqualTo("PUBLISHED");

        AppContent agendado = comunicado();
        agendado.setPublishStartAt(OffsetDateTime.now().plusDays(1));
        assertThat(mapper.toResponse(agendado, PT).getStatus().code()).isEqualTo("SCHEDULED");

        AppContent vencido = comunicado();
        vencido.setPublishEndAt(OffsetDateTime.now().minusMinutes(1));
        assertThat(mapper.toResponse(vencido, PT).getStatus().code()).isEqualTo("EXPIRED");

        AppContent rascunho = comunicado();
        rascunho.setPublished(false);
        // Rascunho com janela aberta continua escondido: o toggle manda.
        rascunho.setPublishStartAt(OffsetDateTime.now().minusDays(1));
        assertThat(mapper.toResponse(rascunho, PT).getStatus().code()).isEqualTo("HIDDEN");
    }

    @Test
    void oRotuloDosEnumsSegueOIdiomaPedido() {
        assertThat(mapper.toResponse(comunicado(), PT).getContentType().label()).isEqualTo("Comunicado");
        assertThat(mapper.toResponse(comunicado(), EN).getContentType().label()).isEqualTo("Announcement");
        assertThat(mapper.toResponse(comunicado(), PT).getAudienceScope().label()).isEqualTo("Todos");
        assertThat(mapper.toResponse(comunicado(), EN).getStatus().label()).isEqualTo("Published");
    }

    @Test
    void oAlvoDaAudienciaSaiComIdENome() {
        AppContent segmentado = comunicado();
        segmentado.setAudienceScope(AudienceScope.COURSE);
        segmentado.setCourse(curso());
        segmentado.setAcademicModule(modulo());
        segmentado.setStudyShift(turno());

        ContentResponse resposta = mapper.toResponse(segmentado, PT);

        assertThat(resposta.getCourseId()).isEqualTo(7);
        assertThat(resposta.getCourseName()).isEqualTo("Administracao");
        assertThat(resposta.getAcademicModuleId()).isEqualTo(3);
        assertThat(resposta.getAcademicModuleName()).isEqualTo("Modulo III");
        assertThat(resposta.getStudyShiftId()).isEqualTo(2);
        assertThat(resposta.getStudyShiftName()).isEqualTo("Noturno");
    }

    @Test
    void semAlvoOsCamposDeAudienciaSaemNulosENaoZerados() {
        ContentResponse resposta = mapper.toResponse(comunicado(), PT);

        assertThat(resposta.getCourseId()).isNull();
        assertThat(resposta.getCourseName()).isNull();
        assertThat(resposta.getAcademicModuleId()).isNull();
        assertThat(resposta.getStudyShiftName()).isNull();
    }

    /**
     * O ano de conclusão é texto no contrato porque o formulário do painel
     * tolera campo vazio (herança do TCC). Nulo tem que sair nulo, não "null".
     */
    @Test
    void oAnoDeConclusaoViraTextoOuSomeDeVez() {
        AppContent trabalho = comunicado();
        trabalho.setContentType(ContentType.WORK);
        trabalho.setCompletionYear(2024);

        assertThat(mapper.toResponse(trabalho, PT).getCompletionYear()).isEqualTo("2024");
        assertThat(mapper.toResponse(comunicado(), PT).getCompletionYear()).isNull();
    }

    @Test
    void oItemDoMuralLevaOTipoComoCodigoESemRotulo() {
        AppContent conteudo = comunicado();
        conteudo.setPinned(true);

        ContentFeedItemResponse item = mapper.toFeedItem(conteudo);

        assertThat(item.getContentType()).isEqualTo("ANNOUNCEMENT");
        assertThat(item.getTitle()).isEqualTo("Biblioteca fechada na sexta");
        assertThat(item.getPinned()).isTrue();
        assertThat(item.getCreatedAt()).isEqualTo(CRIADO_EM);
    }

    @Test
    void oItemDoMuralAguentaConteudoSemTipo() {
        // Linha herdada da migração do TCC pode chegar sem discriminador; o mural
        // não pode responder 500 por causa dela.
        AppContent semTipo = comunicado();
        semTipo.setContentType(null);

        assertThat(mapper.toFeedItem(semTipo).getContentType()).isNull();
    }

    private static AppContent comunicado() {
        return AppContent.builder()
                .id(java.util.UUID.fromString("00000000-0000-4000-8000-000000009001"))
                .contentType(ContentType.ANNOUNCEMENT)
                .title("Biblioteca fechada na sexta")
                .body("Fechamos para inventario do acervo.")
                .published(true)
                .pinned(false)
                .displayOrder(0)
                .audienceScope(AudienceScope.ALL)
                .createdAt(CRIADO_EM)
                .updatedAt(CRIADO_EM)
                .build();
    }

    private static Course curso() {
        Course course = new Course();
        course.setId(7);
        course.setName("Administracao");
        return course;
    }

    private static AcademicModule modulo() {
        AcademicModule module = new AcademicModule();
        module.setId(3);
        module.setName("Modulo III");
        return module;
    }

    private static StudyShift turno() {
        StudyShift shift = new StudyShift();
        shift.setId(2);
        shift.setName("Noturno");
        return shift;
    }

    private static ReloadableResourceBundleMessageSource bundle() {
        ReloadableResourceBundleMessageSource source = new ReloadableResourceBundleMessageSource();
        source.setBasenames("classpath:i18n/enum/messages");
        source.setDefaultEncoding("UTF-8");
        source.setFallbackToSystemLocale(false);
        return source;
    }
}
