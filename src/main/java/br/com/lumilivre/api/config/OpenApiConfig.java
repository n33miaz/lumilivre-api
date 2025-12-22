package br.com.lumilivre.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;

@Configuration
public class OpenApiConfig {

        @Bean
        public OpenAPI customOpenAPI() {
                Server localServer = new Server()
                                .url("http://localhost:8080")
                                .description("Ambiente de Desenvolvimento");

                Server prodServerCustom = new Server()
                                .url("https://lumilivre-api.onrender.com")
                                .description("Ambiente de Produção");

                return new OpenAPI()
                                .info(new Info()
                                                .title("API Lumi Livre")
                                                .version("v1.0-PROD")
                                                .description("API RESTful para o sistema de gerenciamento bibliotecário Lumi Livre. Gerencia acervo, empréstimos, alunos e integrações externas.")
                                                .termsOfService("http://swagger.io/terms/")
                                                .license(new License().name("Apache 2.0").url("http://springdoc.org")))

                                // Configuração da ordem de exibição das TAGs
                                .tags(List.of(
                                                // Acesso
                                                new Tag().name("0. Home")
                                                                .description("Endpoint de verificação de status da API."),
                                                new Tag().name("1. Autenticação")
                                                                .description("Login, recuperação de senha e validação de tokens."),

                                                // Users
                                                new Tag().name("2. Usuários")
                                                                .description("Gestão de contas administrativas (Admin/Bibliotecário)."),
                                                new Tag().name("3. Alunos")
                                                                .description("Cadastro e gestão de alunos, incluindo foto de perfil e resets."),

                                                // Acervo
                                                new Tag().name("4. Livros")
                                                                .description("Catálogo de livros, buscas avançadas, integração com Google Books/BrasilAPI e upload de capas."),
                                                new Tag().name("5. Exemplares")
                                                                .description("Gestão das cópias físicas (tombos), localização na estante e status individual."),
                                                new Tag().name("6. TCC")
                                                                .description("Repositório de Trabalhos de Conclusão de Curso (PDFs e metadados)."),

                                                // Circulação
                                                new Tag().name("7. Solicitações")
                                                                .description("Gestão de reservas e pedidos de empréstimo (fluxo App -> Painel)."),
                                                new Tag().name("8. Empréstimos")
                                                                .description("Registro de saídas, devoluções, renovações e cálculo de multas/penalidades."),

                                                // Tabelas Auxiliares
                                                new Tag().name("9. Cursos")
                                                                .description("Gestão dos cursos oferecidos pela instituição."),
                                                new Tag().name("10. Turnos")
                                                                .description("Gestão dos turnos (Matutino, Vespertino, Noturno, Integral)."),
                                                new Tag().name("11. Módulos")
                                                                .description("Gestão de módulos ou períodos letivos."),
                                                new Tag().name("12. Gêneros")
                                                                .description("Categorias literárias."),
                                                new Tag().name("13. CDD")
                                                                .description("Classificação Decimal Dewey."),

                                                // Ferramentas Administrativas
                                                new Tag().name("14. Relatórios")
                                                                .description("Geração de PDFs (Alunos, Empréstimos, Acervo) e Estatísticas."),
                                                new Tag().name("15. Importação")
                                                                .description("Carga de dados em massa via arquivos Excel (.xlsx)."),
                                                new Tag().name("16. Enums")
                                                                .description("Listagem de valores estáticos do sistema (Status, Penalidades, Tipos de Capa).")))

                                // Lista de servidores
                                .servers(List.of(localServer, prodServerCustom))

                                // Configuração de Segurança JWT Global
                                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                                .components(new Components()
                                                .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                                                .name("bearerAuth")
                                                                .type(SecurityScheme.Type.HTTP)
                                                                .scheme("bearer")
                                                                .bearerFormat("JWT")
                                                                .in(SecurityScheme.In.HEADER)
                                                                .description("Insira o token JWT (sem o prefixo 'Bearer ') para autorizar as requisições.")));
        }
}