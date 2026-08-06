package br.com.lumilivre.api.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import br.com.lumilivre.api.enums.AudienceScope;
import br.com.lumilivre.api.enums.ContentType;
import br.com.lumilivre.api.model.AcademicModule;
import br.com.lumilivre.api.model.AppContent;
import br.com.lumilivre.api.model.Course;
import br.com.lumilivre.api.model.StudyShift;
import br.com.lumilivre.api.repository.AcademicModuleRepository;
import br.com.lumilivre.api.repository.AppContentRepository;
import br.com.lumilivre.api.repository.CourseRepository;
import br.com.lumilivre.api.repository.StudyShiftRepository;
import br.com.lumilivre.api.service.AppContentService;

/**
 * A janela de publicação e a segmentação do mural moram no JPQL do
 * {@code findFeed} — não no serviço. Testar isso com repositório dublê seria
 * afirmar que o Java chama o método certo, e não que o aluno errado deixa de
 * receber o comunicado. Por isso este teste roda contra Postgres de verdade,
 * com o schema das migrations.
 *
 * <p>As linhas do teste levam um marcador no título e as asserções só olham
 * para elas: a seed demo já traz 17 conteúdos cobrindo todos os estados, e o
 * teste não pode quebrar quando ela crescer.
 *
 * <p>Skip silencioso quando o Docker não está acessível, como nos demais testes
 * de integração.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {
        "app.scheduling.enabled=false",
        "jwt.secret=integration-test-secret-with-enough-length-for-hmac-signature-aaaaa",
        "supabase.url=http://localhost:9999",
        "supabase.key=test",
        "supabase.service-role.key=test",
        "lumilivre.storage.provider=local",
        "lumilivre.storage.local.base-dir=./build/storage-content-feed",
        "app.cors.allowed-origins=http://localhost:5173",
        "spring.mail.host=localhost",
        "spring.mail.port=1025",
        "spring.flyway.enabled=true",
        "spring.flyway.locations=classpath:db/migration,classpath:db/seed",
        "spring.jpa.hibernate.ddl-auto=validate"
})
class ContentFeedPostgresTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("lumilivre_feed")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void datasourceProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }

    /** Prefixo que separa as linhas deste teste das 17 da seed demo. */
    private static final String MARK = "ZZFEED";

    @Autowired private AppContentRepository contentRepository;
    @Autowired private CourseRepository courseRepository;
    @Autowired private AcademicModuleRepository academicModuleRepository;
    @Autowired private StudyShiftRepository studyShiftRepository;
    @Autowired private AppContentService contentService;

    private Course meuCurso;
    private Course outroCurso;
    private AcademicModule meuModulo;
    private AcademicModule outroModulo;
    private StudyShift meuTurno;
    private StudyShift outroTurno;

    @BeforeEach
    void limparEPreparar() {
        contentRepository.deleteAll(contentRepository.findAll().stream()
                .filter(c -> c.getTitle() != null && c.getTitle().startsWith(MARK))
                .toList());

        List<Course> cursos = courseRepository.findAll();
        List<AcademicModule> modulos = academicModuleRepository.findAll();
        List<StudyShift> turnos = studyShiftRepository.findAll();
        assertThat(cursos).as("a seed precisa de ao menos dois cursos").hasSizeGreaterThan(1);
        assertThat(modulos).hasSizeGreaterThan(1);
        assertThat(turnos).hasSizeGreaterThan(1);

        meuCurso = cursos.get(0);
        outroCurso = cursos.get(1);
        meuModulo = modulos.get(0);
        outroModulo = modulos.get(1);
        meuTurno = turnos.get(0);
        outroTurno = turnos.get(1);
    }

    /**
     * O rascunho é o caso que a tela do painel cria todo dia: o bibliotecário
     * escreve o comunicado da semana e deixa o toggle desligado até revisar. Se
     * ele aparecesse no mural, "salvar sem publicar" não existiria.
     */
    @Test
    void oMuralNaoEntregaRascunhoNemAgendadoNemVencido() {
        OffsetDateTime agora = OffsetDateTime.now();
        salvar(conteudo("visivel", AudienceScope.ALL));
        salvar(rascunho("rascunho"));
        salvar(janela("agendado", agora.plusDays(1), null));
        salvar(janela("vencido", null, agora.minusMinutes(1)));
        salvar(removido("removido"));
        // Fronteira: janela que abriu há um minuto e fecha daqui a um minuto.
        salvar(janela("na-janela", agora.minusMinutes(1), agora.plusMinutes(1)));

        assertThat(titulosDoFeed(null, null, null))
                .containsExactlyInAnyOrder(titulo("visivel"), titulo("na-janela"));
    }

    @Test
    void aSegmentacaoPorCursoModuloETurnoFiltraCadaUmaNoSeuCampo() {
        salvar(paraCurso("meu-curso", meuCurso));
        salvar(paraCurso("outro-curso", outroCurso));
        salvar(paraModulo("meu-modulo", meuModulo));
        salvar(paraModulo("outro-modulo", outroModulo));
        salvar(paraTurno("meu-turno", meuTurno));
        salvar(paraTurno("outro-turno", outroTurno));
        salvar(conteudo("geral", AudienceScope.ALL));

        assertThat(titulosDoFeed(meuCurso.getId(), meuModulo.getId(), meuTurno.getId()))
                .containsExactlyInAnyOrder(
                        titulo("geral"), titulo("meu-curso"), titulo("meu-modulo"), titulo("meu-turno"));
    }

    /**
     * Leitor sem vínculo acadêmico: o feed recebe três nulos. O risco aqui não é
     * ver de menos, é o {@code = NULL} do SQL virar "casa com qualquer coisa" —
     * e o aluno sem turma receber o comunicado de todas.
     */
    @Test
    void semVinculoAcademicoOLeitorRecebeApenasOMuralGeral() {
        salvar(conteudo("geral", AudienceScope.ALL));
        salvar(paraCurso("curso", meuCurso));
        salvar(paraModulo("modulo", meuModulo));
        salvar(paraTurno("turno", meuTurno));

        assertThat(titulosDoFeed(null, null, null)).containsExactly(titulo("geral"));
    }

    /**
     * A ordem é a do mural: destaque primeiro, depois a ordem manual, e por
     * último o mais recente. É o que o bibliotecário manipula quando quer que o
     * aviso de fechamento fique no topo.
     */
    @Test
    void oDestaqueVemAntesDaOrdemManualQueVemAntesDaData() {
        OffsetDateTime agora = OffsetDateTime.now();
        AppContent fixado = conteudo("fixado", AudienceScope.ALL);
        fixado.setPinned(true);
        fixado.setDisplayOrder(99);
        fixado.setCreatedAt(agora.minusDays(30));
        salvar(fixado);

        AppContent primeiro = conteudo("ordem-1", AudienceScope.ALL);
        primeiro.setDisplayOrder(1);
        primeiro.setCreatedAt(agora.minusDays(10));
        salvar(primeiro);

        AppContent segundoAntigo = conteudo("ordem-2-antigo", AudienceScope.ALL);
        segundoAntigo.setDisplayOrder(2);
        segundoAntigo.setCreatedAt(agora.minusDays(20));
        salvar(segundoAntigo);

        AppContent segundoNovo = conteudo("ordem-2-novo", AudienceScope.ALL);
        segundoNovo.setDisplayOrder(2);
        segundoNovo.setCreatedAt(agora.minusDays(1));
        salvar(segundoNovo);

        assertThat(titulosDoFeed(null, null, null)).containsExactly(
                titulo("fixado"), titulo("ordem-1"), titulo("ordem-2-novo"), titulo("ordem-2-antigo"));
    }

    // ---- listagens do painel (Specification, não JPQL) ------------------------

    /**
     * O painel enxerga rascunho e agendado — é onde eles são editados — mas
     * nunca o removido. O soft delete só serve se a listagem o respeitar.
     */
    @Test
    void aListagemDoPainelMostraORascunhoENuncaORemovido() {
        salvar(conteudo("publicado", AudienceScope.ALL));
        salvar(rascunho("rascunho"));
        salvar(removido("removido"));

        List<String> titulos = contentService.listForAdmin(MARK, null).stream()
                .map(AppContent::getTitle).toList();

        assertThat(titulos).containsExactlyInAnyOrder(titulo("publicado"), titulo("rascunho"));
    }

    @Test
    void aBuscaDoPainelAlcancaTituloEAutor() {
        AppContent porAutor = conteudo("trabalho", AudienceScope.ALL);
        porAutor.setContentType(ContentType.WORK);
        porAutor.setAuthors("Guimarães Rosa");
        salvar(porAutor);
        salvar(conteudo("aviso", AudienceScope.ALL));

        assertThat(contentService.listForAdmin("guimarães", null))
                .extracting(AppContent::getTitle).containsExactly(titulo("trabalho"));
        assertThat(contentService.listForAdmin(MARK, ContentType.WORK))
                .extracting(AppContent::getTitle).containsExactly(titulo("trabalho"));
    }

    /**
     * O filtro avançado combina tipo, escopo, curso e ano. Cada campo ausente
     * tem de sumir do WHERE: era esse o padrão {@code (:p IS NULL OR ...)} que
     * quebrava no Postgres e virou Specification.
     */
    @Test
    void oFiltroAvancadoCombinaOsCamposEIgnoraOsAusentes() {
        AppContent trabalho = paraCurso("tcc-2024", meuCurso);
        trabalho.setContentType(ContentType.WORK);
        trabalho.setCompletionYear(2024);
        salvar(trabalho);

        AppContent outroAno = paraCurso("tcc-2023", meuCurso);
        outroAno.setContentType(ContentType.WORK);
        outroAno.setCompletionYear(2023);
        salvar(outroAno);

        salvar(conteudo("aviso", AudienceScope.ALL));

        assertThat(contentService.searchAdvanced(ContentType.WORK, AudienceScope.COURSE, meuCurso.getId(), "2024"))
                .extracting(AppContent::getTitle).containsExactly(titulo("tcc-2024"));
        assertThat(contentService.searchAdvanced(ContentType.WORK, null, null, null))
                .extracting(AppContent::getTitle)
                .contains(titulo("tcc-2024"), titulo("tcc-2023"))
                .doesNotContain(titulo("aviso"));
        assertThat(contentService.searchAdvanced(null, AudienceScope.COURSE, outroCurso.getId(), null))
                .extracting(AppContent::getTitle).doesNotContain(titulo("tcc-2024"));
    }

    // ---- helpers -------------------------------------------------------------

    private List<String> titulosDoFeed(Integer cursoId, Integer moduloId, Integer turnoId) {
        return contentRepository.findFeed(cursoId, moduloId, turnoId, OffsetDateTime.now()).stream()
                .map(AppContent::getTitle)
                .filter(t -> t.startsWith(MARK))
                .toList();
    }

    private void salvar(AppContent content) {
        contentRepository.saveAndFlush(content);
    }

    private static String titulo(String sufixo) {
        return MARK + "-" + sufixo;
    }

    private static AppContent conteudo(String sufixo, AudienceScope scope) {
        return AppContent.builder()
                .contentType(ContentType.ANNOUNCEMENT)
                .title(titulo(sufixo))
                .body("Corpo do comunicado.")
                .published(true)
                .pinned(false)
                .displayOrder(0)
                .audienceScope(scope)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
    }

    private static AppContent rascunho(String sufixo) {
        AppContent content = conteudo(sufixo, AudienceScope.ALL);
        content.setPublished(false);
        return content;
    }

    private static AppContent removido(String sufixo) {
        AppContent content = conteudo(sufixo, AudienceScope.ALL);
        content.setDeletedAt(OffsetDateTime.now().minusHours(1));
        return content;
    }

    private static AppContent janela(String sufixo, OffsetDateTime inicio, OffsetDateTime fim) {
        AppContent content = conteudo(sufixo, AudienceScope.ALL);
        content.setPublishStartAt(inicio);
        content.setPublishEndAt(fim);
        return content;
    }

    private static AppContent paraCurso(String sufixo, Course curso) {
        AppContent content = conteudo(sufixo, AudienceScope.COURSE);
        content.setCourse(curso);
        return content;
    }

    private static AppContent paraModulo(String sufixo, AcademicModule modulo) {
        AppContent content = conteudo(sufixo, AudienceScope.MODULE);
        content.setAcademicModule(modulo);
        return content;
    }

    private static AppContent paraTurno(String sufixo, StudyShift turno) {
        AppContent content = conteudo(sufixo, AudienceScope.SHIFT);
        content.setStudyShift(turno);
        return content;
    }
}
