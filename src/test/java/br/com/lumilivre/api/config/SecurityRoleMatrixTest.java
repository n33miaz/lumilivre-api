package br.com.lumilivre.api.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
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

import br.com.lumilivre.api.controller.LoanController;
import br.com.lumilivre.api.controller.BookController;
import br.com.lumilivre.api.controller.ReservationController;
import br.com.lumilivre.api.controller.LoanRequestController;
import br.com.lumilivre.api.controller.system.AppUserController;
import br.com.lumilivre.api.enums.Role;
import br.com.lumilivre.api.model.AppUser;
import br.com.lumilivre.api.model.Loan;
import br.com.lumilivre.api.model.Student;
import br.com.lumilivre.api.repository.LoanRepository;
import br.com.lumilivre.api.security.CustomUserDetails;
import br.com.lumilivre.api.security.CustomUserDetailsService;
import br.com.lumilivre.api.security.JwtAuthenticationFilter;
import br.com.lumilivre.api.security.JwtUtil;
import br.com.lumilivre.api.security.StudentAuthorizationService;
import br.com.lumilivre.api.service.BookService;
import br.com.lumilivre.api.service.LoanRequestService;
import br.com.lumilivre.api.service.LoanService;
import br.com.lumilivre.api.service.RecommendationService;
import br.com.lumilivre.api.service.AppUserService;
import br.com.lumilivre.api.service.ReservationService;

@WebMvcTest(controllers = {
        BookController.class,
        LoanController.class,
        LoanRequestController.class,
        ReservationController.class,
        AppUserController.class
})
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        StudentAuthorizationService.class
})
class SecurityRoleMatrixTest {

    private static final String STUDENT_MATRICULA = "12345";
    private static final UUID BOOK_ID = UUID.fromString("00000000-0000-0000-0000-000000000010");
    private static final UUID OWN_LOAN_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID OTHER_LOAN_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID RESERVATION_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BookService livroService;

    @MockBean
    private RecommendationService recommendationService;

    @MockBean
    private LoanService loanService;

    @MockBean
    private LoanRequestService loanRequestService;

    @MockBean
    private ReservationService reservationService;

    @MockBean
    private AppUserService usuarioService;

    @MockBean
    private LoanRepository loanRepository;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @BeforeEach
    void setUpServiceDefaults() {
        when(livroService.buscarCatalogoParaMobile()).thenReturn(List.of());
        when(livroService.findById(any())).thenReturn(Optional.empty());
        when(livroService.buscarParaListaAdmin(any(Pageable.class))).thenReturn(Page.empty());

        when(loanService.gerarRankingAlunos(anyInt(), any(), any(), any())).thenReturn(List.of());
        when(loanService.buscarEmprestimoParaListaAdmin(any(Pageable.class))).thenReturn(Page.empty());
        when(loanService.listarEmprestimosAluno(anyString())).thenReturn(List.of());

        when(loanRequestService.solicitarEmprestimo(anyString(), anyString()))
                .thenReturn(ResponseEntity.ok("Solicitacao registrada"));
        when(loanRequestService.solicitarEmprestimoPorLivro(anyString(), any()))
                .thenReturn(ResponseEntity.ok("Solicitacao registrada"));

        when(usuarioService.buscarUsuarioParaListaAdmin(any(Pageable.class))).thenReturn(Page.empty());

        when(loanRepository.findById(OWN_LOAN_ID)).thenReturn(Optional.of(loan(STUDENT_MATRICULA)));
        when(loanRepository.findById(OTHER_LOAN_ID)).thenReturn(Optional.of(loan("99999")));
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
                EndpointCase.get("detalhe livro", "/livros/" + BOOK_ID,
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
                        "/solicitacoes/solicitar-mobile?matriculaAluno=" + STUDENT_MATRICULA + "&livroId=" + BOOK_ID,
                        null,
                        AccessExpectation.UNAUTHORIZED, AccessExpectation.ALLOWED,
                        AccessExpectation.ALLOWED, AccessExpectation.ALLOWED),
                EndpointCase.post("solicitar emprestimo mobile outro aluno",
                        "/solicitacoes/solicitar-mobile?matriculaAluno=99999&livroId=" + BOOK_ID,
                        null,
                        AccessExpectation.UNAUTHORIZED, AccessExpectation.FORBIDDEN,
                        AccessExpectation.ALLOWED, AccessExpectation.ALLOWED),
                EndpointCase.put("renovar emprestimo proprio", "/emprestimos/renovar/" + OWN_LOAN_ID, null,
                        AccessExpectation.UNAUTHORIZED, AccessExpectation.ALLOWED,
                        AccessExpectation.ALLOWED, AccessExpectation.ALLOWED),
                EndpointCase.put("renovar emprestimo outro aluno", "/emprestimos/renovar/" + OTHER_LOAN_ID, null,
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
                        "/reservas?matricula=" + STUDENT_MATRICULA + "&livroId=" + BOOK_ID,
                        null,
                        AccessExpectation.UNAUTHORIZED, AccessExpectation.ALLOWED,
                        AccessExpectation.ALLOWED, AccessExpectation.ALLOWED),
                EndpointCase.post("reserva outro aluno",
                        "/reservas?matricula=99999&livroId=" + BOOK_ID,
                        null,
                        AccessExpectation.UNAUTHORIZED, AccessExpectation.FORBIDDEN,
                        AccessExpectation.ALLOWED, AccessExpectation.ALLOWED),
                EndpointCase.delete("cancelar reserva proprio aluno",
                        "/reservas/" + RESERVATION_ID + "/cancelar?matricula=" + STUDENT_MATRICULA,
                        AccessExpectation.UNAUTHORIZED, AccessExpectation.ALLOWED,
                        AccessExpectation.ALLOWED, AccessExpectation.ALLOWED),
                EndpointCase.delete("cancelar reserva outro aluno",
                        "/reservas/" + RESERVATION_ID + "/cancelar?matricula=99999",
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

    private static Loan loan(String matricula) {
        Loan loan = new Loan();
        Student student = new Student();
        student.setRegistrationNumber(matricula);
        loan.setStudent(student);
        return loan;
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
        usuario.setId(new UUID(0L, actor.ordinal()));
        usuario.setEmail(actor.name().toLowerCase() + "@lumilivre.test");
        usuario.setPasswordHash("{noop}password");
        usuario.setRole(role);

        if (role == Role.STUDENT) {
            Student aluno = new Student();
            aluno.setRegistrationNumber(STUDENT_MATRICULA);
            usuario.setStudent(aluno);
        }

        return new CustomUserDetails(usuario);
    }
}
