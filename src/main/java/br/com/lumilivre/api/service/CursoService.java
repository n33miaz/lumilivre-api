package br.com.lumilivre.api.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import br.com.lumilivre.api.model.CursoModel;
import br.com.lumilivre.api.model.ResponseModel;
import br.com.lumilivre.api.repository.CursoRepository;

@Service
public class CursoService {

    @Autowired
    private CursoRepository cr;

    @Autowired
    private ResponseModel rm;


    // public List<CursoModel> listar() {
    //     return cr.findAll();
    // }

    public List<CursoModel> listar() {
        return (List<CursoModel>) cr.findAll();
    }

    public ResponseEntity<?> cadastrar(CursoModel cursoModel) {
        if (isNomeInvalido(cursoModel)) {
            rm.setMensagem("O Nome é Obrigatório");
            return ResponseEntity.badRequest().body(rm);
        }
        CursoModel salvo = cr.save(cursoModel);
        return ResponseEntity.status(HttpStatus.CREATED).body(salvo);
    }

    public ResponseEntity<?> alterar(CursoModel cursoModel) {
        if (isNomeInvalido(cursoModel)) {
            rm.setMensagem("O Nome é Obrigatório");
            return ResponseEntity.badRequest().body(rm);
        }
        CursoModel salvo = cr.save(cursoModel);
        return ResponseEntity.ok(salvo);
    }
    public ResponseEntity<ResponseModel> delete(Long id) {
        cr.deleteById(id);
        rm.setMensagem("O Curso foi removido com sucesso");
        return new ResponseEntity<ResponseModel>(rm, HttpStatus.OK);
    }
    private boolean isNomeInvalido(CursoModel cursoModel) {
        return cursoModel.getNome() == null || cursoModel.getNome().trim().isEmpty();
    }
}
