package br.com.lumilivre.api.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    private AutorRepository ar;

    public Page<AutorModel> buscarPorTexto(String texto, Pageable pageable) {
        if (texto == null || texto.isBlank()) {
            return ar.findAll(pageable);
        }
        return ar.buscarPorTexto(texto, pageable);
    }

    public Page<AutorModel> buscarAvancado(
            String nome,
            String pseudonimo,
            String nacionalidade,

            Pageable pageable) {
        return ar.buscarAvancado(nome, pseudonimo, nacionalidade, pageable);
    }

    public ResponseEntity<?> cadastrar(AutorModel autorModel) {
        if (isNomeInvalido(autorModel)) {
            return badRequest("O Nome do autor é obrigatório.");
        }

        if (autorModel.getCodigo() == null || autorModel.getCodigo().trim().isEmpty()) {
            return badRequest("O código do autor é obrigatório.");
        }

        if (ar.existsById(autorModel.getCodigo())) {
            return badRequest("Já existe um autor com este código.");
        }

        AutorModel salvo = ar.save(autorModel);
        return ResponseEntity.status(HttpStatus.CREATED).body(salvo);
    }

    public ResponseEntity<?> atualizar(AutorModel autorModel) {
        if (isNomeInvalido(autorModel)) {
            return badRequest("O Nome do autor é obrigatório.");
        }

        if (autorModel.getCodigo() == null || autorModel.getCodigo().trim().isEmpty()) {
            return badRequest("O código do autor é obrigatório.");
        }

        Optional<AutorModel> existente = ar.findById(autorModel.getCodigo());
        if (existente.isEmpty()) {
            return notFound("Autor não encontrado com o código: " + autorModel.getCodigo());
        }

        AutorModel salvo = ar.save(autorModel);
        return ResponseEntity.ok(salvo);
    }

    public ResponseEntity<ResponseModel> excluir(String codigo) {
        if (!ar.existsById(codigo)) {
            return notFound("Autor não encontrado com o código: " + codigo);
        }

        ar.deleteById(codigo);
        ResponseModel rm = new ResponseModel();
        rm.setMensagem("O autor foi removido com sucesso.");
        return ResponseEntity.ok(rm);
    }

    private boolean isNomeInvalido(AutorModel autorModel) {
        return autorModel.getNome() == null || autorModel.getNome().trim().isEmpty();
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

    public AutorModel buscarPorNome(String nome) {
        return ar.findByNome(nome).orElse(null);
    }

    public AutorModel buscarPorCodigo(String codigo) {
        return ar.findByCodigo(codigo);
    }
}