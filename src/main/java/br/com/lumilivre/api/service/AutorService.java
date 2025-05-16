package br.com.lumilivre.api.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import br.com.lumilivre.api.model.AutorModel;
import br.com.lumilivre.api.model.ResponseModel;
import br.com.lumilivre.api.repository.AutorRepository;
@Service
public class AutorService {

    @Autowired
    private AutorRepository ar;

    @Autowired
    private ResponseModel rm;

    public List<AutorModel> listar() {
        return (List<AutorModel>) ar.findAll();
    }

    public ResponseEntity<?> cadastrar(AutorModel autorModel) {
        if (isNomeInvalido(autorModel)) {
            rm.setMensagem("O Nome é Obrigatório");
            return ResponseEntity.badRequest().body(rm);
        }
        AutorModel salvo = ar.save(autorModel);
        return ResponseEntity.status(HttpStatus.CREATED).body(salvo);
    }

    public ResponseEntity<?> alterar(AutorModel autorModel) {
        if (isNomeInvalido(autorModel)) {
            rm.setMensagem("O Nome é Obrigatório");
            return ResponseEntity.badRequest().body(rm);
        }
        AutorModel salvo = ar.save(autorModel);
        return ResponseEntity.ok(salvo);
    }
    public ResponseEntity<ResponseModel> delete(String codigo) {
        ar.deleteById(codigo);
        rm.setMensagem("O Autor foi removido com sucesso");
        return new ResponseEntity<ResponseModel>(rm, HttpStatus.OK);
    }
    private boolean isNomeInvalido(AutorModel autorModel) {
        return autorModel.getNome() == null || autorModel.getNome().trim().isEmpty();
    }
}
