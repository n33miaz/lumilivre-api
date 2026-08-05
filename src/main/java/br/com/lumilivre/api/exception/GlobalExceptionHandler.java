package br.com.lumilivre.api.exception;

import br.com.lumilivre.api.config.MessageResolver;
import br.com.lumilivre.api.domain.policy.BookAvailabilityPolicy.BookAvailabilityViolationException;
import br.com.lumilivre.api.domain.policy.LoanPolicy.LoanPolicyViolationException;
import br.com.lumilivre.api.domain.policy.PasswordPolicy.PasswordPolicyViolationException;
import br.com.lumilivre.api.domain.policy.RequestApprovalPolicy.RequestApprovalViolationException;
import br.com.lumilivre.api.dto.common.ErrorResponse;
import br.com.lumilivre.api.exception.custom.BusinessRuleException;
import br.com.lumilivre.api.exception.custom.MessageKeyedException;
import br.com.lumilivre.api.exception.custom.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final MessageResolver messages;

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex, Locale locale, WebRequest request) {
        Map<String, String> violations = ex.getBindingResult().getFieldErrors().stream()
            .collect(Collectors.toMap(
                FieldError::getField,
                FieldError::getDefaultMessage,
                (a, b) -> a
            ));
        ErrorResponse body = ErrorResponse.builder()
            .status(HttpStatus.BAD_REQUEST.value())
            .error(messages.resolve("error.validation.title", locale))
            .message(messages.resolve("error.validation.message", locale))
            .path(extractPath(request))
            .correlationId(MDC.get("correlationId"))
            .violations(violations)
            .build();
        return ResponseEntity.badRequest()
            .header("Content-Language", locale.toLanguageTag())
            .body(body);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(
            ResourceNotFoundException ex, Locale locale, WebRequest request) {
        String msg = ex.hasI18nKey()
            ? messages.resolve(ex.getMessageKey(), locale, ex.getMessageArgs())
            : ex.getMessage();
        return errorResponse(HttpStatus.NOT_FOUND,
            messages.resolve("error.resource-not-found.title", locale), msg, locale, request);
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ErrorResponse> handleBusinessRule(
            BusinessRuleException ex, Locale locale, WebRequest request) {
        String msg = ex.hasI18nKey()
            ? messages.resolve(ex.getMessageKey(), locale, ex.getMessageArgs())
            : ex.getMessage();
        return errorResponse(HttpStatus.BAD_REQUEST,
            messages.resolve("error.business-rule.title", locale), msg, locale, request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex, Locale locale, WebRequest request) {
        return errorResponse(HttpStatus.BAD_REQUEST,
            messages.resolve("error.invalid-request.title", locale), ex.getMessage(), locale, request);
    }

    /** Método HTTP inexistente na rota: 405, nunca 500 do handler genérico. */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException ex, Locale locale, WebRequest request) {
        ResponseEntity<ErrorResponse> response = errorResponse(HttpStatus.METHOD_NOT_ALLOWED,
            messages.resolve("error.method-not-allowed.title", locale),
            messages.resolve("error.method-not-allowed.message", locale), locale, request);
        if (ex.getSupportedHttpMethods() == null || ex.getSupportedHttpMethods().isEmpty()) {
            return response;
        }
        // Allow é obrigatório num 405 (RFC 9110).
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
            .headers(headers -> headers.setAllow(ex.getSupportedHttpMethods()))
            .header("Content-Language", locale.toLanguageTag())
            .body(response.getBody());
    }

    @ExceptionHandler({
        LoanPolicyViolationException.class,
        BookAvailabilityViolationException.class,
        RequestApprovalViolationException.class
    })
    public ResponseEntity<ErrorResponse> handlePolicyViolation(
            RuntimeException ex, Locale locale, WebRequest request) {
        String msg = ex instanceof MessageKeyedException keyed && keyed.hasI18nKey()
            ? messages.resolve(keyed.getMessageKey(), locale, keyed.getMessageArgs())
            : ex.getMessage();
        return errorResponse(HttpStatus.UNPROCESSABLE_ENTITY,
            messages.resolve("error.policy-violation.title", locale), msg, locale, request);
    }

    @ExceptionHandler(LockedException.class)
    public ResponseEntity<ErrorResponse> handleLocked(
            LockedException ex, Locale locale, WebRequest request) {
        // Conta temporariamente bloqueada por excesso de tentativas.
        ErrorResponse body = ErrorResponse.builder()
                .status(HttpStatus.TOO_MANY_REQUESTS.value())
                .error(messages.resolve("error.too-many-attempts.title", locale))
                .message(messages.resolve("error.too-many-attempts.message", locale))
                .path(extractPath(request))
                .correlationId(MDC.get("correlationId"))
                .build();
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header("Content-Language", locale.toLanguageTag())
                .header("Retry-After", "900")
                .body(body);
    }

    /**
     * Senha fraca é erro de entrada do usuário, não conflito de estado: 400 com
     * a mensagem específica, para o formulário dizer o que corrigir.
     */
    @ExceptionHandler(PasswordPolicyViolationException.class)
    public ResponseEntity<ErrorResponse> handlePasswordPolicy(
            PasswordPolicyViolationException ex, Locale locale, WebRequest request) {
        return errorResponse(HttpStatus.BAD_REQUEST,
            messages.resolve("error.validation.title", locale),
            messages.resolve(ex.getMessageKey(), locale, ex.getMessageArgs()), locale, request);
    }

    /**
     * Conta desativada/bloqueada: a senha estava certa, o acesso é que foi
     * retirado. 403 em vez de 401 para o cliente não tentar renovar credencial.
     */
    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ErrorResponse> handleDisabledAccount(
            DisabledException ex, Locale locale, WebRequest request) {
        return errorResponse(HttpStatus.FORBIDDEN,
            messages.resolve("error.access-denied.title", locale),
            messages.resolve(ex.getMessage(), locale), locale, request);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(
            AuthenticationException ex, Locale locale, WebRequest request) {
        return errorResponse(HttpStatus.UNAUTHORIZED,
            messages.resolve("error.unauthorized.title", locale),
            messages.resolve("error.unauthorized.message", locale), locale, request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            AccessDeniedException ex, Locale locale, WebRequest request) {
        return errorResponse(HttpStatus.FORBIDDEN,
            messages.resolve("error.access-denied.title", locale),
            messages.resolve("error.access-denied.message", locale), locale, request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobal(
            Exception ex, Locale locale, WebRequest request) {
        ex.printStackTrace();
        return errorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
            messages.resolve("error.internal.title", locale),
            messages.resolve("error.internal.message", locale), locale, request);
    }

    private ResponseEntity<ErrorResponse> errorResponse(
            HttpStatus status, String error, String message, Locale locale, WebRequest request) {
        ErrorResponse body = ErrorResponse.builder()
            .status(status.value())
            .error(error)
            .message(message)
            .path(extractPath(request))
            .correlationId(MDC.get("correlationId"))
            .build();
        return ResponseEntity.status(status)
            .header("Content-Language", locale.toLanguageTag())
            .body(body);
    }

    private String extractPath(WebRequest request) {
        return request.getDescription(false).replace("uri=", "");
    }
}
