package br.com.lumilivre.api.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import br.com.lumilivre.api.controller.EmprestimoController;
import br.com.lumilivre.api.controller.LivroController;
import br.com.lumilivre.api.controller.ReservaController;
import br.com.lumilivre.api.controller.SolicitacaoEmprestimoController;
import br.com.lumilivre.api.controller.system.AppUserController;
import br.com.lumilivre.api.enums.Role;
import br.com.lumilivre.api.model.Student;
import br.com.lumilivre.api.model.EmprestimoModel;
import br.com.lumilivre.api.model.AppUser;
import br.com.lumilivre.api.repository.EmprestimoRepository;
import br.com.lumilivre.api.security.CustomUserDetails;
import br.com.lumilivre.api.security.CustomUserDetailsService;
import br.com.lumilivre.api.security.JwtAuthenticationFilter;
import br.com.lumilivre.api.security.JwtUtil;
import br.com.lumilivre.api.security.StudentAuthorizationService;
import br.com.lumilivre.api.service.EmprestimoService;
import br.com.lumilivre.api.service.LivroService;
import br.com.lumilivre.api.service.RecomendacaoService;
import br.com.lumilivre.api.service.ReservaService;
import br.com.lumilivre.api.service.SolicitacaoEmprestimoService;
import br.com.lumilivre.api.service.AppUserService;

@WebMvcTest(controllers = {
        LivroController.class,
        EmprestimoController.class,
        SolicitacaoEmprestimoController.class,
        ReservaController.class,
        AppUserController.class
})
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        StudentAuthorizationService.class
})
class SecurityRoleMatrixTest {

    private static final String STUDENT_MATRICULA = "12345";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LivroService livroService;

    @MockBean
    private RecomendacaoService recomendacaoService;

    @MockBean
    private EmprestimoService emprestimoService;

    @MockBean
    private SolicitacaoEmprestimoService solicitacaoEmprestimoService;

    @MockBean
    private ReservaService reservaService;

    @MockBean
    private AppUserService usuarioService;

    @MockBean
    private EmprestimoRepository emprestimoRepository;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @BeforeEach
    void setUpServiceDefaults() {
        when(livroService.buscarCatalogoParaMobile()).thenReturn(List.of());
        when(livroService.findById(any())).thenReturn(Optional.empty());
        when(livroService.buscarParaListaAdmin(any(Pageable.class))).thenReturn(Page.empty());

        when(emprestimoService.gerarRankingAlunos(anyInt(), any(), any(), any())).thenReturn(List.of());
        when(emprestimoService.buscarEmprestimoParaListaAdmin(any(Pageable.class))).thenReturn(Page.empty());
        when(emprestimoService.listarEmprestimosAluno(anyString())).thenReturn(List.of());

        when(solicitacaoEmprestimoService.solicitarEmprestimo(anyString(), anyString()))
                .thenReturn(ResponseEntity.ok("Solicitacao registrada"));
        when(solicitacaoEmprestimoService.solicitarEmprestimoPorLivro(anyString(), any()))
                .thenReturn(ResponseEntity.ok("Solicitacao registrada"));

        when(usuarioService.buscarUsuarioParaListaAdmin(any(Pageable.class))).thenReturn(Page.empty());

        when(emprestimoRepository.findById(1)).thenReturn(Optional.of(emprestimo(STUDENT_MATRICULA)));
        when(emprestimoRepository.findById(2)).thenReturn(Optional.of(emprestimo("99999")));
    }

    @ParameterizedTest(name = "{0} as {1} => {2}")
    @MethodSource("roleMatrix")
    @DisplayName("aplica matriz de autorizacao por rota e papel")
    void shouldApplySecurityRoleMatrix(EndpointCase endpointCase, Actor actor, AccessExpectation expectation)
            throws Exception {
        int status = mockMvc.perform(endpointCase.requestFor(actor)).andReturn().getResponse().getStatus();

        switch (expectation) {
            case ALLOWED -> assertThat(status).isNotIn(401, 403);
            case UNAUTHORIZED -> assertThat(status).isEqualTo(401);
            case FORBIDDEN -> assertThat(status).isEqualTo(403);
        }
    }

    static Stream<Arguments> roleMatrix() {
        EndpointCase[] endpoints = new EndpointCase[] {
                EndpointCase.get("catalogo publico", "/livros/catalogo-mobile",
                        AccessExpectation.ALLOWED, AccessExpectation.ALLOWED,
                        AccessExpectation.ALLOWED, AccessExpectation.ALLOWED),
                EndpointCase.get("detalhe livro", "/livros/1",
                        AccessExpectation.UNAUTHORIZED, AccessExpectation.ALLOWED,
                        AccessExpectation.ALLOWED, AccessExpectation.ALLOWED),
                EndpointCase.get("livros admin", "/livros/home",
                        AccessExpectation.UNAUTHORIZED, AccessExpectation.FORBIDDEN,
                        AccessExpectation.ALLOWED, AccessExpectation.ALLOWED),
                EndpointCase.get("ranking emprestimos", "/emprestimos/ranking",
                        AccessExpectation.UNAUTHORIZED, AccessExpectation.ALLOWED,
                        AccessExpectation.ALLOWED, AccessExpectation.ALLOWED),
                EndpointCase.get("emprestimos admin", "/emprestimos/home",
                        AccessExpectation.UNAUTHORIZED, AccessExpectation.FORBIDDEN,
                        AccessExpectation.ALLOWED, AccessExpectation.ALLOWED),
                EndpointCase.get("emprestimos proprio aluno", "/emprestimos/aluno/" + STUDENT_MATRICULA,
                        AccessExpectation.UNAUTHORIZED, AccessExpectation.ALLOWED,
                        AccessExpectation.ALLOWED, AccessExpectation.ALLOWED),
                EndpointCase.get("emprestimos outro aluno", "/emprestimos/aluno/99999",
                        AccessExpectation.UNAUTHORIZED, AccessExpectation.FORBIDDEN,
                        AccessExpectation.ALLOWED, AccessExpectation.ALLOWED),
                EndpointCase.post("solicitar emprestimo",
                        "/solicitacoes/solicitar?matriculaAluno=" + STUDENT_MATRICULA + "&tomboExemplar=T001",
                        null,
                        AccessExpectation.UNAUTHORIZED, AccessExpectation.ALLOWED,
                        AccessExpectation.ALLOWED, AccessExpectation.ALLOWED),
                EndpointCase.post("solicitar emprestimo outro aluno",
                        "/solicitacoes/solicitar?matriculaAluno=99999&tomboExemplar=T001",
                        null,
                        AccessExpectation.UNAUTHORIZED, AccessExpectation.FORBIDDEN,
                        AccessExpectation.ALLOWED, AccessExpectation.ALLOWED),
                EndpointCase.post("solicitar emprestimo mobile",
                        "/solicitacoes/solicitar-mobile?matriculaAluno=" + STUDENT_MATRICULA + "&livroId=1",
                        null,
                        AccessExpectation.UNAUTHORIZED, AccessExpectation.ALLOWED,
                        AccessExpectation.ALLOWED, AccessExpectation.ALLOWED),
                EndpointCase.post("solicitar emprestimo mobile outro aluno",
                        "/solicitacoes/solicitar-mobile?matriculaAluno=99999&livroId=1",
                        null,
                        AccessExpectation.UNAUTHORIZED, AccessExpectation.FORBIDDEN,
                        AccessExpectation.ALLOWED, AccessExpectation.ALLOWED),
                EndpointCase.put("renovar emprestimo proprio", "/emprestimos/renovar/1", null,
                        AccessExpectation.UNAUTHORIZED, AccessExpectation.ALLOWED,
                        AccessExpectation.ALLOWED, AccessExpectation.ALLOWED),
                EndpointCase.put("renovar emprestimo outro aluno", "/emprestimos/renovar/2", null,
                        AccessExpectation.UNAUTHORIZED, AccessExpectation.FORBIDDEN,
                        AccessExpectation.ALLOWED, AccessExpectation.ALLOWED),
                EndpointCase.get("recomendacoes proprio aluno",
                        "/livros/mobile/recomendacoes/" + STUDENT_MATRICULA,
                        AccessExpectation.UNAUTHORIZED, AccessExpectation.ALLOWED,
                        AccessExpectation.ALLOWED, AccessExpectation.ALLOWED),
                EndpointCase.get("recomendacoes outro aluno",
                        "/livros/mobile/recomendacoes/99999",
                        AccessExpectation.UNAUTHORIZED, AccessExpectation.FORBIDDEN,
                        AccessExpectation.ALLOWED, AccessExpectation.ALLOWED),
                EndpointCase.post("reserva proprio aluno",
                        "/reservas?matricula=" + STUDENT_MATRICULA + "&livroId=1",
                        null,
                        AccessExpectation.UNAUTHORIZED, AccessExpectation.ALLOWED,
                        AccessExpectation.ALLOWED, AccessExpectation.ALLOWED),
                EndpointCase.post("reserva outro aluno",
                        "/reservas?matricula=99999&livroId=1",
                        null,
                        AccessExpectation.UNAUTHORIZED, AccessExpectation.FORBIDDEN,
                        AccessExpectation.ALLOWED, AccessExpectation.ALLOWED),
                EndpointCase.delete("cancelar reserva proprio aluno",
                        "/reservas/1/cancelar?matricula=" + STUDENT_MATRICULA,
                        AccessExpectation.UNAUTHORIZED, AccessExpectation.ALLOWED,
                        AccessExpectation.ALLOWED, AccessExpectation.ALLOWED),
                EndpointCase.delete("cancelar reserva outro aluno",
                        "/reservas/1/cancelar?matricula=99999",
                        AccessExpectation.UNAUTHORIZED, AccessExpectation.FORBIDDEN,
                        AccessExpectation.ALLOWED, AccessExpectation.ALLOWED),
                EndpointCase.get("usuarios home", "/usuarios/home",
                        AccessExpectation.UNAUTHORIZED, AccessExpectation.FORBIDDEN,
                        AccessExpectation.FORBIDDEN, AccessExpectation.ALLOWED),
                EndpointCase.put("alterar propria senha", "/usuarios/alterar-senha",
                        "{\"senhaAtual\":\"a\",\"novaSenha\":\"b\"}",
                        AccessExpectation.UNAUTHORIZED, AccessExpectation.ALLOWED,
                        AccessExpectation.ALLOWED, AccessExpectation.ALLOWED)
        };

        return Stream.of(endpoints)
                .flatMap(endpoint -> Stream.of(
                        Arguments.of(endpoint, Actor.ANONYMOUS, endpoint.anonymousExpectation()),
                        Arguments.of(endpoint, Actor.ALUNO, endpoint.studentExpectation()),
                        Arguments.of(endpoint, Actor.BIBLIOTECARIO, endpoint.librarianExpectation()),
                        Arguments.of(endpoint, Actor.ADMIN, endpoint.adminExpectation())));
    }

    private enum AccessExpectation {
        ALLOWED,
        UNAUTHORIZED,
        FORBIDDEN
    }

    private enum Actor {
        ANONYMOUS,
        ALUNO,
        BIBLIOTECARIO,
        ADMIN
    }

    private static EmprestimoModel emprestimo(String matricula) {
        EmprestimoModel emprestimo = new EmprestimoModel();
        Student aluno = new Student();
        aluno.setMatricula(matricula);
        emprestimo.setAluno(aluno);
        return emprestimo;
    }

    private record EndpointCase(
            String name,
            String method,
            String path,
            String body,
            AccessExpectation anonymousExpectation,
            AccessExpectation studentExpectation,
            AccessExpectation librarianExpectation,
            AccessExpectation adminExpectation) {

        static EndpointCase get(
                String name,
                String path,
                AccessExpectation anonymousExpectation,
                AccessExpectation studentExpectation,
                AccessExpectation librarianExpectation,
                AccessExpectation adminExpectation) {
            return new EndpointCase(name, "GET", path, null, anonymousExpectation,
                    studentExpectation, librarianExpectation, adminExpectation);
        }

        static EndpointCase post(
                String name,
                String path,
                String body,
                AccessExpectation anonymousExpectation,
                AccessExpectation studentExpectation,
                AccessExpectation librarianExpectation,
                AccessExpectation adminExpectation) {
            return new EndpointCase(name, "POST", path, body, anonymousExpectation,
                    studentExpectation, librarianExpectation, adminExpectation);
        }

        static EndpointCase put(
                String name,
                String path,
                String body,
                AccessExpectation anonymousExpectation,
                AccessExpectation studentExpectation,
                AccessExpectation librarianExpectation,
                AccessExpectation adminExpectation) {
            return new EndpointCase(name, "PUT", path, body, anonymousExpectation,
                    studentExpectation, librarianExpectation, adminExpectation);
        }

        static EndpointCase delete(
                String name,
                String path,
                AccessExpectation anonymousExpectation,
                AccessExpectation studentExpectation,
                AccessExpectation librarianExpectation,
                AccessExpectation adminExpectation) {
            return new EndpointCase(name, "DELETE", path, null, anonymousExpectation,
                    studentExpectation, librarianExpectation, adminExpectation);
        }

        RequestBuilder requestFor(Actor actor) {
            MockHttpServletRequestBuilder request = switch (method) {
                case "POST" -> MockMvcRequestBuilders.post(path);
                case "PUT" -> MockMvcRequestBuilders.put(path);
                case "DELETE" -> MockMvcRequestBuilders.delete(path);
                default -> MockMvcRequestBuilders.get(path);
            };

            if (body != null) {
                request.contentType(MediaType.APPLICATION_JSON).content(body);
            }

            if (actor != Actor.ANONYMOUS) {
                request.with(user(userDetailsFor(actor)));
            }

            return request;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private static UserDetails userDetailsFor(Actor actor) {
        Role role = switch (actor) {
            case ADMIN -> Role.ADMIN;
            case BIBLIOTECARIO -> Role.LIBRARIAN;
            case ALUNO -> Role.STUDENT;
            case ANONYMOUS -> throw new IllegalArgumentException("Anonymous actor has no principal");
        };

        AppUser usuario = new AppUser();
        usuario.setId(actor.ordinal());
        usuario.setEmail(actor.name().toLowerCase() + "@lumilivre.test");
        usuario.setSenha("{noop}password");
        usuario.setRole(role);

        if (role == Role.STUDENT) {
            Student aluno = new Student();
            aluno.setMatricula(STUDENT_MATRICULA);
            usuario.setAluno(aluno);
        }

        return new CustomUserDetails(usuario);
    }
}
