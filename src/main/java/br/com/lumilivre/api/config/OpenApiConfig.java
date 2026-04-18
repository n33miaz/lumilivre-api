package br.com.lumilivre.api.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(buildInfo())
                .tags(buildTags())
                .servers(buildServers())
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(buildComponents());
    }

    private Info buildInfo() {
        return new Info()
                .title("LumiLivre API")
                .version("v1.0-PROD")
                .description("""
                        API RESTful para o sistema de gerenciamento bibliotecário **LumiLivre**.

                        Gerencia acervo, empréstimos, alunos, TCCs e integrações externas (Google Books, BrasilAPI, Supabase Storage).

                        **Autenticação:** Bearer JWT — obtenha o token em `POST /auth/login`.

                        **Roles:**
                        - `ADMIN` — acesso total
                        - `BIBLIOTECARIO` — acesso operacional (empréstimos, alunos, livros)
                        - `ALUNO` — acesso ao próprio perfil e catálogo
                        """)
                .contact(new Contact()
                        .name("LumiLivre")
                        .email("contato.lumilivre@gmail.com"))
                .license(new License().name("Apache 2.0").url("https://www.apache.org/licenses/LICENSE-2.0"));
    }

    private List<Tag> buildTags() {
        return List.of(
                new Tag().name("0. Home").description("Endpoint de verificação de status da API."),
                new Tag().name("1. Autenticação").description("Login, recuperação de senha e validação de tokens."),
                new Tag().name("2. Usuários").description("Gestão de contas administrativas (Admin/Bibliotecário)."),
                new Tag().name("3. Alunos").description("Cadastro e gestão de alunos, incluindo foto de perfil e resets."),
                new Tag().name("4. Livros").description("Catálogo, buscas avançadas, integração Google Books/BrasilAPI e upload de capas."),
                new Tag().name("5. Exemplares").description("Cópias físicas (tombos), localização na estante e status individual."),
                new Tag().name("6. TCC").description("Repositório de Trabalhos de Conclusão de Curso (PDFs e metadados)."),
                new Tag().name("7. Solicitações").description("Pedidos de empréstimo (fluxo App → Painel)."),
                new Tag().name("8. Empréstimos").description("Registro de saídas, devoluções e cálculo de penalidades."),
                new Tag().name("9. Cursos").description("Cursos da instituição."),
                new Tag().name("10. Turnos").description("Turnos: Matutino, Vespertino, Noturno, Integral."),
                new Tag().name("11. Módulos").description("Módulos / períodos letivos."),
                new Tag().name("12. Gêneros").description("Categorias literárias."),
                new Tag().name("13. CDD").description("Classificação Decimal Dewey."),
                new Tag().name("14. Relatórios").description("Geração de PDFs e estatísticas."),
                new Tag().name("15. Importação").description("Carga em massa via Excel (.xlsx)."),
                new Tag().name("16. Enums").description("Valores estáticos do sistema (status, penalidades, tipos de capa).")
        );
    }

    private List<Server> buildServers() {
        return List.of(
                new Server().url("http://localhost:8080").description("Desenvolvimento"),
                new Server().url("https://lumilivre-api.onrender.com").description("Produção")
        );
    }

    private Components buildComponents() {
        return new Components()
                .addSecuritySchemes("bearerAuth", new SecurityScheme()
                        .name("bearerAuth")
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .in(SecurityScheme.In.HEADER)
                        .description("Token JWT retornado por `POST /auth/login`. Insira apenas o valor (sem 'Bearer ')."))
                .addRequestBodies("LoginRequest", buildLoginRequestBody())
                .addRequestBodies("EsqueciSenhaRequest", buildEsqueciSenhaBody());
    }

    private RequestBody buildLoginRequestBody() {
        return new RequestBody()
                .description("Credenciais de login")
                .required(true)
                .content(new Content().addMediaType("application/json",
                        new MediaType()
                                .schema(new Schema<>().$ref("#/components/schemas/LoginRequest"))
                                .addExamples("admin", new Example()
                                        .summary("Admin/Bibliotecário")
                                        .value("{\"login\":\"admin@lumilivre.com.br\",\"senha\":\"senha123\"}"))
                                .addExamples("aluno", new Example()
                                        .summary("Aluno (matrícula)")
                                        .value("{\"login\":\"2024001\",\"senha\":\"2024001\"}"))));
    }

    private RequestBody buildEsqueciSenhaBody() {
        return new RequestBody()
                .description("Email para recuperação de senha")
                .required(true)
                .content(new Content().addMediaType("application/json",
                        new MediaType()
                                .schema(new Schema<>().type("object")
                                        .addProperty("email", new Schema<>().type("string").format("email")))
                                .addExamples("exemplo", new Example()
                                        .value("{\"email\":\"aluno@exemplo.com\"}"))));
    }
}
