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
                Server localServer = new Server().url("http://localhost:8080").description("Ambiente de Desenvolvimento Local");
                Server prodServerAws = new Server().url("http://18.229.126.245:8080").description("Ambiente de Produção (AWS)");
                Server prodServerCustom = new Server().url("http://api.lumilivre.com.br:8080").description("Ambiente de Produção (Domínio)");

        return new OpenAPI()
                .info(new Info()
                        .title("API Lumi Livre")
                        .version("v1.0-PROD")
                        .description("API RESTful para o sistema de gerenciamento bibliotecário Lumi Livre. Esta documentação descreve todos os endpoints disponíveis.")
                        .termsOfService("http://swagger.io/terms/")
                        .license(new License().name("Apache 2.0").url("http://springdoc.org")))
                
                .tags(List.of( // ordem de exibição no swagger ui + paragrafo e descrição
                        new Tag().name("1. Autenticação").description("Endpoints para obter tokens de acesso"),
                        new Tag().name("2. Alunos").description("Operações para gerenciamento de alunos"),
                        new Tag().name("3. Livros").description("Operações para gerenciamento de livros e consulta de disponibilidade"),
                        new Tag().name("4. Autores").description("Operações para gerenciamento de autores"),
                        new Tag().name("5. Cursos").description("Operações para gerenciamento de cursos")
                ))

                // lista de servidores
                .servers(List.of(localServer, prodServerAws, prodServerCustom))

                // configura a segurança JWT para o swagger
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .name("bearerAuth")
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .in(SecurityScheme.In.HEADER)
                                .description("Insira o token JWT aqui para autorizar as requisições.")));
        }
}