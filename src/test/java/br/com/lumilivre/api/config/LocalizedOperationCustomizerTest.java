package br.com.lumilivre.api.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.method.HandlerMethod;

class LocalizedOperationCustomizerTest {

    @Test
    void publicOperationIsLocalizedAndDoesNotRequireBearerSecurity() throws Exception {
        Operation operation = new Operation()
                .operationId("auth.login")
                .summary("old summary")
                .description("old description")
                .tags(List.of("auth"))
                .responses(new ApiResponses().addApiResponse("200", new ApiResponse().description("ok")));

        Operation result = customizer().customize(operation, handler("login"));

        assertThat(result.getSummary()).isEqualTo("Localized login summary");
        assertThat(result.getDescription()).isEqualTo("Localized login description");
        assertThat(result.getTags()).containsExactly("Authentication");
        assertThat(result.getSecurity()).isEmpty();
        assertThat(result.getParameters()).extracting(Parameter::get$ref)
                .contains("#/components/parameters/AcceptLanguage");
        assertThat(result.getResponses()).containsKeys("200", "400", "429");
        assertThat(result.getResponses()).doesNotContainKeys("401", "403");
        assertThat(result.getResponses().get("200").getHeaders()).containsKeys(
                "Content-Language", "X-Correlation-Id");
    }

    @Test
    void protectedMutationGetsSecurityAndDefaultErrorResponses() throws Exception {
        Operation operation = new Operation()
                .operationId("books.update")
                .tags(List.of("books"))
                .addParametersItem(new Parameter().name("id").description("old id"))
                .requestBody(new RequestBody().description("old body"))
                .responses(new ApiResponses().addApiResponse("200", new ApiResponse().description("old ok")));

        Operation result = customizer().customize(operation, handler("update"));

        assertThat(result.getSummary()).isEqualTo("Update a book");
        assertThat(result.getDescription()).isEqualTo("Updates catalog metadata");
        assertThat(result.getSecurity()).hasSize(1);
        assertThat(result.getSecurity().get(0).get("bearerAuth")).isNotNull();
        assertThat(result.getRequestBody().getDescription()).isEqualTo("Book payload");
        assertThat(result.getParameters()).filteredOn(parameter -> "id".equals(parameter.getName()))
                .singleElement()
                .extracting(Parameter::getDescription)
                .isEqualTo("Book id");
        assertThat(result.getResponses()).containsKeys("400", "401", "403", "404", "422", "429");
        assertThat(result.getResponses().get("200").getDescription()).isEqualTo("Updated");
    }

    @Test
    void missingOperationIdIsDerivedFromTagAndHandlerMethod() throws Exception {
        Operation operation = new Operation()
                .tags(List.of(SwaggerTags.SYSTEM))
                .responses(new ApiResponses());

        Operation result = customizer().customize(operation, handler("create"));

        assertThat(result.getOperationId()).isEqualTo(SwaggerTags.SYSTEM + ".create");
        assertThat(result.getTags()).containsExactly("System");
    }

    @Test
    void existingAcceptLanguageParameterIsNotDuplicated() throws Exception {
        Operation operation = new Operation()
                .operationId("books.catalog")
                .addParametersItem(new Parameter().$ref("#/components/parameters/AcceptLanguage"));

        Operation result = customizer().customize(operation, handler("login"));

        assertThat(result.getParameters())
                .filteredOn(parameter -> "#/components/parameters/AcceptLanguage".equals(parameter.get$ref()))
                .hasSize(1);
    }

    private static LocalizedOperationCustomizer customizer() {
        StaticMessageSource messages = new StaticMessageSource();
        Locale locale = Locale.US;
        messages.addMessage("swagger.operation.auth.login.summary", locale, "Localized login summary");
        messages.addMessage("swagger.operation.auth.login.description", locale, "Localized login description");
        messages.addMessage("swagger.operation.books.update.summary", locale, "Update a book");
        messages.addMessage("swagger.operation.books.update.description", locale, "Updates catalog metadata");
        messages.addMessage("swagger.parameter.books.update.id.description", locale, "Book id");
        messages.addMessage("swagger.requestBody.books.update.description", locale, "Book payload");
        messages.addMessage("swagger.response.books.update.200.description", locale, "Updated");
        messages.addMessage("swagger.tag.auth.name", locale, "Authentication");
        messages.addMessage("swagger.tag.books.name", locale, "Books");
        messages.addMessage("swagger.tag." + SwaggerTags.SYSTEM + ".name", locale, "System");
        return new LocalizedOperationCustomizer(messages, locale);
    }

    private static HandlerMethod handler(String methodName) throws Exception {
        Method method = DummyController.class.getDeclaredMethod(methodName);
        return new HandlerMethod(new DummyController(), method);
    }

    private static final class DummyController {
        void login() {
        }

        @PostMapping
        void create() {
        }

        @PutMapping
        void update() {
        }
    }
}
