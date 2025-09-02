package br.com.lumilivre.api.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import br.com.lumilivre.api.data.RespostaErroDTO;

@ControllerAdvice
public class TratadorDeExcecoesGlobal {

    // captura exceções genéricas e retorna HTTP 400
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<RespostaErroDTO> tratarIllegalArgumentException(IllegalArgumentException ex, WebRequest request) {
        
        RespostaErroDTO respostaErro = new RespostaErroDTO(
            HttpStatus.BAD_REQUEST.value(),
            "Requisição Inválida",
            ex.getMessage(), // Usa a mensagem da própria exceção
            request.getDescription(false).replace("uri=", "")
        );

        return new ResponseEntity<>(respostaErro, HttpStatus.BAD_REQUEST);
    }
    
    // captura qualquer outra exceção que não foi tratada especificamente e retorna HTTP 500
    @ExceptionHandler(Exception.class)
    public ResponseEntity<RespostaErroDTO> tratarExcecaoGlobal(Exception ex, WebRequest request) {

        RespostaErroDTO respostaErro = new RespostaErroDTO(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "Erro Interno do Servidor",
            "Ocorreu um erro inesperado. Por favor, tente novamente mais tarde.", // mensagem genérica 
            request.getDescription(false).replace("uri=", "")
        );
        
        ex.printStackTrace(); 

        return new ResponseEntity<>(respostaErro, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}