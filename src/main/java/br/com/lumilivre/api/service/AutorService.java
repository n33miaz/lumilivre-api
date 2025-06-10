package br.com.lumilivre.api.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import br.com.lumilivre.api.model.AutorModel;
import br.com.lumilivre.api.model.ResponseModel;
import br.com.lumilivre.api.repository.AutorRepository;

import java.util.Optional;

@Service
public class AutorService {

    @Autowired
    private AutorRepository autorRepository;

    public Iterable<AutorModel> listar() {
        return autorRepository.findAll();
    }

    public ResponseEntity<?> cadastrar(AutorModel autorModel) {
        if (isNomeInvalido(autorModel)) {
            return badRequest("O Nome do autor é obrigatório.");
        }

        if (isSobrenomeInvalido(autorModel)) {
            return badRequest("O Sobrenome do autor é obrigatório.");
        }

        if (autorModel.getCodigo() == null || autorModel.getCodigo().trim().isEmpty()) {
            return badRequest("O código do autor é obrigatório.");
        }

        if (autorRepository.existsById(autorModel.getCodigo())) {
            return badRequest("Já existe um autor com este código.");
        }

        AutorModel salvo = autorRepository.save(autorModel);
        return ResponseEntity.status(HttpStatus.CREATED).body(salvo);
    }

    public ResponseEntity<?> alterar(AutorModel autorModel) {
        if (isNomeInvalido(autorModel)) {
            return badRequest("O Nome do autor é obrigatório.");
        }

        if (isSobrenomeInvalido(autorModel)) {
            return badRequest("O Sobrenome do autor é obrigatório.");
        }

        if (autorModel.getCodigo() == null || autorModel.getCodigo().trim().isEmpty()) {
            return badRequest("O código do autor é obrigatório.");
        }

        Optional<AutorModel> existente = autorRepository.findById(autorModel.getCodigo());
        if (existente.isEmpty()) {
            return notFound("Autor não encontrado com o código: " + autorModel.getCodigo());
        }

        AutorModel salvo = autorRepository.save(autorModel);
        return ResponseEntity.ok(salvo);
    }

    public ResponseEntity<ResponseModel> delete(String codigo) {
        if (!autorRepository.existsById(codigo)) {
            return notFound("Autor não encontrado com o código: " + codigo);
        }

        autorRepository.deleteById(codigo);
        ResponseModel rm = new ResponseModel();
        rm.setMensagem("O autor foi removido com sucesso.");
        return ResponseEntity.ok(rm);
    }

    private boolean isNomeInvalido(AutorModel autorModel) {
        return autorModel.getNome() == null || autorModel.getNome().trim().isEmpty();
    }

    private boolean isSobrenomeInvalido(AutorModel autorModel) {
        return autorModel.getSobrenome() == null || autorModel.getSobrenome().trim().isEmpty();
    }

    private ResponseEntity<ResponseModel> badRequest(String mensagem) {
        ResponseModel rm = new ResponseModel();
        rm.setMensagem(mensagem);
        return ResponseEntity.badRequest().body(rm);
    }

    private ResponseEntity<ResponseModel> notFound(String mensagem) {
        ResponseModel rm = new ResponseModel();
        rm.setMensagem(mensagem);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(rm);
    }
}
