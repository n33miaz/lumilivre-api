package br.com.lumilivre.api.exception;

import br.com.lumilivre.api.dto.RespostaErroDTO;
import br.com.lumilivre.api.exception.custom.RecursoNaoEncontradoException;
import br.com.lumilivre.api.exception.custom.RegraDeNegocioException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.util.Map;
import java.util.stream.Collectors;

@ControllerAdvice
public class TratadorDeExcecoesGlobal {

    // Retorna HTTP 400
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> tratarValidacao(MethodArgumentNotValidException ex) {
        Map<String, String> erros = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        fieldError -> fieldError.getField(),
                        fieldError -> fieldError.getDefaultMessage()));
        Map<String, Object> corpoResposta = Map.of("status", 400, "erro", "Erro de Validação", "mensagens", erros);
        return new ResponseEntity<>(corpoResposta, HttpStatus.BAD_REQUEST);
    }

    // Retorna HTTP 404
    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<RespostaErroDTO> tratarRecursoNaoEncontrado(RecursoNaoEncontradoException ex,
            WebRequest request) {
        RespostaErroDTO resposta = new RespostaErroDTO(
                HttpStatus.NOT_FOUND.value(),
                "Recurso Não Encontrado",
                ex.getMessage(),
                request.getDescription(false).replace("uri=", ""));
        return new ResponseEntity<>(resposta, HttpStatus.NOT_FOUND);
    }

    // Retorna HTTP 400
    @ExceptionHandler(RegraDeNegocioException.class)
    public ResponseEntity<RespostaErroDTO> tratarRegraDeNegocio(RegraDeNegocioException ex, WebRequest request) {
        RespostaErroDTO resposta = new RespostaErroDTO(
                HttpStatus.BAD_REQUEST.value(),
                "Violação de Regra de Negócio",
                ex.getMessage(),
                request.getDescription(false).replace("uri=", ""));
        return new ResponseEntity<>(resposta, HttpStatus.BAD_REQUEST);
    }

    // Retorna HTTP 400
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<RespostaErroDTO> tratarIllegalArgumentException(IllegalArgumentException ex,
            WebRequest request) {
        RespostaErroDTO resposta = new RespostaErroDTO(
                HttpStatus.BAD_REQUEST.value(),
                "Requisição Inválida",
                ex.getMessage(),
                request.getDescription(false).replace("uri=", ""));
        return new ResponseEntity<>(resposta, HttpStatus.BAD_REQUEST);
    }

    // Retorna HTTP 401
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<RespostaErroDTO> tratarAuthenticationException(AuthenticationException ex,
            WebRequest request) {
        RespostaErroDTO resposta = new RespostaErroDTO(
                HttpStatus.UNAUTHORIZED.value(),
                "Não Autenticado",
                "Token de autenticação inválido, expirado ou ausente.",
                request.getDescription(false).replace("uri=", ""));
        return new ResponseEntity<>(resposta, HttpStatus.UNAUTHORIZED);
    }

    // Retorna HTTP 403
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<RespostaErroDTO> tratarAccessDeniedException(AccessDeniedException ex, WebRequest request) {
        RespostaErroDTO resposta = new RespostaErroDTO(
                HttpStatus.FORBIDDEN.value(),
                "Acesso Negado",
                "Você não tem permissão para acessar este recurso.",
                request.getDescription(false).replace("uri=", ""));
        return new ResponseEntity<>(resposta, HttpStatus.FORBIDDEN);
    }

    // Retorna HTTP 500
    @ExceptionHandler(Exception.class)
    public ResponseEntity<RespostaErroDTO> tratarExcecaoGlobal(Exception ex, WebRequest request) {
        ex.printStackTrace();

        RespostaErroDTO resposta = new RespostaErroDTO(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Erro Interno do Servidor",
                "Ocorreu um erro inesperado. Por favor, contate o suporte.",
                request.getDescription(false).replace("uri=", ""));
        return new ResponseEntity<>(resposta, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}