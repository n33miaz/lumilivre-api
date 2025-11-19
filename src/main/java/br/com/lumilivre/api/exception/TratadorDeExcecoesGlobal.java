package br.com.lumilivre.api.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import br.com.lumilivre.api.dto.RespostaErroDTO;

import java.util.Map;
import java.util.stream.Collectors;

@ControllerAdvice
public class TratadorDeExcecoesGlobal {

    // erros de validação
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> tratarValidacao(MethodArgumentNotValidException ex) {
        Map<String, String> erros = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        fieldError -> fieldError.getField(),
                        fieldError -> fieldError.getDefaultMessage()));
        Map<String, Object> corpoResposta = Map.of("status", 400, "erro", "Erro de Validação", "mensagens", erros);
        return new ResponseEntity<>(corpoResposta, HttpStatus.BAD_REQUEST);
    }

    // erros de requisição
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<RespostaErroDTO> tratarIllegalArgumentException(IllegalArgumentException ex,
            WebRequest request) {
        RespostaErroDTO resposta = new RespostaErroDTO(400, "Requisição Inválida", ex.getMessage(),
                request.getDescription(false).replace("uri=", ""));
        return new ResponseEntity<>(resposta, HttpStatus.BAD_REQUEST);
    }

    // falha de autenticação
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<RespostaErroDTO> tratarAuthenticationException(AuthenticationException ex,
            WebRequest request) {
        RespostaErroDTO resposta = new RespostaErroDTO(401, "Não Autenticado",
                "Token de autenticação inválido, expirado ou ausente.",
                request.getDescription(false).replace("uri=", ""));
        return new ResponseEntity<>(resposta, HttpStatus.UNAUTHORIZED);
    }

    // acesso negado
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<RespostaErroDTO> tratarAccessDeniedException(AccessDeniedException ex, WebRequest request) {
        RespostaErroDTO resposta = new RespostaErroDTO(403, "Acesso Negado",
                "Você não tem permissão para acessar este recurso.", request.getDescription(false).replace("uri=", ""));
        return new ResponseEntity<>(resposta, HttpStatus.FORBIDDEN);
    }

    // pega o restante dos erros
    @ExceptionHandler(Exception.class)
    public ResponseEntity<RespostaErroDTO> tratarExcecaoGlobal(Exception ex, WebRequest request) {
        ex.printStackTrace();
        RespostaErroDTO resposta = new RespostaErroDTO(500, "Erro Interno do Servidor", "Ocorreu um erro inesperado.",
                request.getDescription(false).replace("uri=", ""));
        return new ResponseEntity<>(resposta, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}