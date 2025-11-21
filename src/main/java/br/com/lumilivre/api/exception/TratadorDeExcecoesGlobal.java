package br.com.lumilivre.api.exception;

import br.com.lumilivre.api.dto.comum.ErroResponse;
import br.com.lumilivre.api.exception.custom.RecursoNaoEncontradoException;
import br.com.lumilivre.api.exception.custom.RegraDeNegocioException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.util.Map;
import java.util.stream.Collectors;

@ControllerAdvice
public class TratadorDeExcecoesGlobal {

        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<Map<String, Object>> tratarValidacao(MethodArgumentNotValidException ex) {
                Map<String, String> erros = ex.getBindingResult().getFieldErrors().stream()
                                .collect(Collectors.toMap(
                                                FieldError::getField,
                                                FieldError::getDefaultMessage));

                Map<String, Object> corpoResposta = Map.of(
                                "status", HttpStatus.BAD_REQUEST.value(),
                                "erro", "Erro de Validação",
                                "mensagens", erros);

                return ResponseEntity.badRequest().body(corpoResposta);
        }

        @ExceptionHandler(RecursoNaoEncontradoException.class)
        public ResponseEntity<ErroResponse> tratarRecursoNaoEncontrado(RecursoNaoEncontradoException ex,
                        WebRequest request) {
                return criarResposta(HttpStatus.NOT_FOUND, "Recurso Não Encontrado", ex.getMessage(), request);
        }

        @ExceptionHandler(RegraDeNegocioException.class)
        public ResponseEntity<ErroResponse> tratarRegraDeNegocio(RegraDeNegocioException ex, WebRequest request) {
                return criarResposta(HttpStatus.BAD_REQUEST, "Violação de Regra de Negócio", ex.getMessage(), request);
        }

        @ExceptionHandler(IllegalArgumentException.class)
        public ResponseEntity<ErroResponse> tratarIllegalArgumentException(IllegalArgumentException ex,
                        WebRequest request) {
                return criarResposta(HttpStatus.BAD_REQUEST, "Requisição Inválida", ex.getMessage(), request);
        }

        @ExceptionHandler(AuthenticationException.class)
        public ResponseEntity<ErroResponse> tratarAuthenticationException(AuthenticationException ex,
                        WebRequest request) {
                return criarResposta(HttpStatus.UNAUTHORIZED, "Não Autenticado",
                                "Token de autenticação inválido, expirado ou ausente.", request);
        }

        @ExceptionHandler(AccessDeniedException.class)
        public ResponseEntity<ErroResponse> tratarAccessDeniedException(AccessDeniedException ex, WebRequest request) {
                return criarResposta(HttpStatus.FORBIDDEN, "Acesso Negado",
                                "Você não tem permissão para acessar este recurso.", request);
        }

        @ExceptionHandler(Exception.class)
        public ResponseEntity<ErroResponse> tratarExcecaoGlobal(Exception ex, WebRequest request) {
                ex.printStackTrace();

                return criarResposta(HttpStatus.INTERNAL_SERVER_ERROR, "Erro Interno do Servidor",
                                "Ocorreu um erro inesperado. Por favor, contate o suporte.", request);
        }

        private ResponseEntity<ErroResponse> criarResposta(HttpStatus status, String tituloErro,
                        String mensagemDetalhada, WebRequest request) {
                ErroResponse erro = ErroResponse.builder()
                                .status(status.value())
                                .erro(tituloErro)
                                .mensagem(mensagemDetalhada)
                                .caminho(extrairCaminho(request))
                                .build();

                return ResponseEntity.status(status).body(erro);
        }

        private String extrairCaminho(WebRequest request) {
                return request.getDescription(false).replace("uri=", "");
        }
}