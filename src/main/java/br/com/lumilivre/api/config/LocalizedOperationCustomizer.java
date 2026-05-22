package br.com.lumilivre.api.config;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.MessageSource;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.method.HandlerMethod;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class LocalizedOperationCustomizer implements OperationCustomizer {

    private static final Set<String> PUBLIC_OPERATION_IDS = Set.of(
            "auth.login",
            "auth.forgotPassword",
            "auth.validateResetToken",
            "auth.resetPassword",
            "books.publicSearch",
            "books.catalog",
            "books.byGenre",
            "system.home"
    );

    private final MessageSource messages;
    private final Locale locale;

    public LocalizedOperationCustomizer(MessageSource messages, Locale locale) {
        this.messages = messages;
        this.locale = locale;
    }

    @Override
    public Operation customize(Operation operation, HandlerMethod handlerMethod) {
        String operationId = operation.getOperationId();
        if (operationId == null || operationId.isBlank()) {
            operationId = deriveOperationId(operation, handlerMethod);
            operation.setOperationId(operationId);
        }

        if (operationId != null && operationId.contains(".")) {
            operation.setSummary(resolve("swagger.operation." + operationId + ".summary", operation.getSummary()));
            operation.setDescription(resolve("swagger.operation." + operationId + ".description", operation.getDescription()));
            ensureAcceptLanguageParameter(operation);
            localizeParameters(operation, operationId);
            localizeRequestBody(operation, operationId);
            localizeResponses(operation, operationId, handlerMethod);
            applySecurity(operation, operationId);
        }

        if (operation.getTags() != null) {
            operation.setTags(operation.getTags().stream()
                    .map(this::localizeTag)
                    .toList());
        }

        return operation;
    }

    private String deriveOperationId(Operation operation, HandlerMethod handlerMethod) {
        String tag = operation.getTags() != null && !operation.getTags().isEmpty()
                ? operation.getTags().get(0)
                : SwaggerTags.SYSTEM;
        return tag + "." + handlerMethod.getMethod().getName();
    }

    private void localizeParameters(Operation operation, String operationId) {
        if (operation.getParameters() == null) {
            return;
        }
        operation.getParameters().forEach(parameter -> {
            if (parameter.get$ref() != null) {
                return;
            }
            String specific = "swagger.parameter." + operationId + "." + parameter.getName() + ".description";
            String common = "swagger.parameter.common." + parameter.getName() + ".description";
            parameter.setDescription(resolveCascade(specific, common, parameter.getDescription()));
        });
    }

    private void ensureAcceptLanguageParameter(Operation operation) {
        if (operation.getParameters() == null) {
            operation.setParameters(new ArrayList<>());
        }
        boolean alreadyPresent = operation.getParameters().stream()
                .anyMatch(parameter -> "Accept-Language".equalsIgnoreCase(parameter.getName())
                        || "#/components/parameters/AcceptLanguage".equals(parameter.get$ref()));
        if (!alreadyPresent) {
            operation.addParametersItem(new Parameter().$ref("#/components/parameters/AcceptLanguage"));
        }
    }

    private void localizeRequestBody(Operation operation, String operationId) {
        if (operation.getRequestBody() == null) {
            return;
        }
        operation.getRequestBody().setDescription(resolve(
                "swagger.requestBody." + operationId + ".description",
                operation.getRequestBody().getDescription()));
    }

    private void localizeResponses(Operation operation, String operationId, HandlerMethod handlerMethod) {
        if (operation.getResponses() == null) {
            operation.setResponses(new ApiResponses());
        }

        ensureDefaultResponses(operation, operationId, handlerMethod);

        operation.getResponses().forEach((status, response) -> {
            String specific = "swagger.response." + operationId + "." + status + ".description";
            String common = "swagger.response.common." + status + ".description";
            response.setDescription(resolveCascade(specific, common, response.getDescription()));
            ensureResponseHeaders(response);
        });
    }

    private void ensureResponseHeaders(ApiResponse response) {
        if (response.get$ref() != null) {
            return;
        }
        if (response.getHeaders() == null || !response.getHeaders().containsKey("Content-Language")) {
            response.addHeaderObject("Content-Language", new Header().$ref("#/components/headers/Content-Language"));
        }
        if (response.getHeaders() == null || !response.getHeaders().containsKey("X-Correlation-Id")) {
            response.addHeaderObject("X-Correlation-Id", new Header().$ref("#/components/headers/X-Correlation-Id"));
        }
    }

    private void ensureDefaultResponses(Operation operation, String operationId, HandlerMethod handlerMethod) {
        ApiResponses responses = operation.getResponses();
        if (!responses.containsKey("400")) {
            responses.addApiResponse("400", new ApiResponse().$ref("#/components/responses/BadRequest"));
        }
        if (!PUBLIC_OPERATION_IDS.contains(operationId)) {
            if (!responses.containsKey("401")) {
                responses.addApiResponse("401", new ApiResponse().$ref("#/components/responses/Unauthorized"));
            }
            if (!responses.containsKey("403")) {
                responses.addApiResponse("403", new ApiResponse().$ref("#/components/responses/Forbidden"));
            }
        }
        if (!responses.containsKey("429")) {
            responses.addApiResponse("429", new ApiResponse().$ref("#/components/responses/RateLimited"));
        }
        if (hasPathVariableLikeOperation(operationId) && !responses.containsKey("404")) {
            responses.addApiResponse("404", new ApiResponse().$ref("#/components/responses/NotFound"));
        }
        if (isMutation(handlerMethod) && !responses.containsKey("422")) {
            responses.addApiResponse("422", new ApiResponse().$ref("#/components/responses/BusinessRuleViolation"));
        }
    }

    private boolean hasPathVariableLikeOperation(String operationId) {
        return operationId.endsWith(".get")
                || operationId.endsWith(".getOne")
                || operationId.endsWith(".update")
                || operationId.endsWith(".delete")
                || operationId.endsWith(".cancel")
                || operationId.endsWith(".close")
                || operationId.endsWith(".renew")
                || operationId.endsWith(".isbnLookup")
                || operationId.endsWith(".postalCode");
    }

    private boolean isMutation(HandlerMethod handlerMethod) {
        return handlerMethod.hasMethodAnnotation(PostMapping.class)
                || handlerMethod.hasMethodAnnotation(PutMapping.class)
                || handlerMethod.hasMethodAnnotation(PatchMapping.class)
                || handlerMethod.hasMethodAnnotation(DeleteMapping.class);
    }

    private void applySecurity(Operation operation, String operationId) {
        if (PUBLIC_OPERATION_IDS.contains(operationId)) {
            operation.setSecurity(List.of());
            return;
        }
        if (operation.getSecurity() == null || operation.getSecurity().isEmpty()) {
            operation.setSecurity(List.of(new SecurityRequirement().addList("bearerAuth")));
        }
    }

    private String localizeTag(String tag) {
        return messages.getMessage("swagger.tag." + tag + ".name", null, tag, locale);
    }

    private String resolve(String key, String fallback) {
        return messages.getMessage(key, null, fallback != null ? fallback : "", locale);
    }

    private String resolveCascade(String specificKey, String commonKey, String fallback) {
        String specific = messages.getMessage(specificKey, null, null, locale);
        if (specific != null && !specific.isBlank()) {
            return specific;
        }
        return messages.getMessage(commonKey, null, fallback != null ? fallback : "", locale);
    }
}
