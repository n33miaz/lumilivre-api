package br.com.lumilivre.api.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import br.com.lumilivre.api.model.GeneroModel;
import br.com.lumilivre.api.model.ResponseModel;
import br.com.lumilivre.api.repository.GeneroRepository;

@Service
public class GeneroService {

    @Autowired
    private GeneroRepository gr;

    @Autowired
    private ResponseModel rm;

    public List<GeneroModel> listar() {
        return (List<GeneroModel>) gr.findAll();
    }

    public ResponseEntity<?> cadastrar(GeneroModel GeneroModel) {
        if (isNomeInvalido(GeneroModel)) {
            rm.setMensagem("O Nome é Obrigatório");
            return ResponseEntity.badRequest().body(rm);
        }
        GeneroModel salvo = gr.save(GeneroModel);
        return ResponseEntity.status(HttpStatus.CREATED).body(salvo);
    }

    public ResponseEntity<?> alterar(GeneroModel GeneroModel) {
        if (isNomeInvalido(GeneroModel)) {
            rm.setMensagem("O Nome é Obrigatório");
            return ResponseEntity.badRequest().body(rm);
        }
        GeneroModel salvo = gr.save(GeneroModel);
        return ResponseEntity.ok(salvo);
    }

    public ResponseEntity<ResponseModel> delete(Long id) {
        gr.deleteById(id);
        rm.setMensagem("O Genero foi removido com sucesso");
        return new ResponseEntity<ResponseModel>(rm, HttpStatus.OK);
    }

    private boolean isNomeInvalido(GeneroModel GeneroModel) {
        return GeneroModel.getNome() == null || GeneroModel.getNome().trim().isEmpty();
    }
}
