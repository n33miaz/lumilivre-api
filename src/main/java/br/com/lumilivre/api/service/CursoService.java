package br.com.lumilivre.api.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import br.com.lumilivre.api.model.CursoModel;
import br.com.lumilivre.api.model.ResponseModel;
import br.com.lumilivre.api.repository.CursoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CursoService {

    @Autowired
    private CursoRepository cr;

    public List<CursoModel> listar() {
        return (List<CursoModel>) cr.findAll();
    }

    @Transactional
    public ResponseEntity<?> cadastrar(CursoModel cursoModel) {
        if (isNomeInvalido(cursoModel)) {
            ResponseModel rm = new ResponseModel();
            rm.setMensagem("O Nome é Obrigatório");
            return ResponseEntity.badRequest().body(rm);
        }

        if (cr.existsByNomeIgnoreCase(cursoModel.getNome())) {
            ResponseModel rm = new ResponseModel();
            rm.setMensagem("O Nome já existe no banco de dados");
            return ResponseEntity.badRequest().body(rm);
        }

        CursoModel salvo = cr.save(cursoModel);
        return ResponseEntity.status(HttpStatus.CREATED).body(salvo);
    }

    @Transactional
    public ResponseEntity<?> alterar(CursoModel cursoModel) {
        if (isNomeInvalido(cursoModel)) {
            ResponseModel rm = new ResponseModel();
            rm.setMensagem("O Nome é Obrigatório");
            return ResponseEntity.badRequest().body(rm);
        }

        if (cr.existsByNomeIgnoreCaseAndIdNot(cursoModel.getNome(), cursoModel.getId())) {
            ResponseModel rm = new ResponseModel();
            rm.setMensagem("O Nome já existe no banco de dados");
            return ResponseEntity.badRequest().body(rm);
        }

        CursoModel salvo = cr.save(cursoModel);
        return ResponseEntity.ok(salvo);
    }

    @Transactional
    public ResponseEntity<ResponseModel> delete(Integer id) {
        cr.deleteById(id);
        ResponseModel rm = new ResponseModel();
        rm.setMensagem("O Curso foi removido com sucesso");
        return new ResponseEntity<>(rm, HttpStatus.OK);
    }

    private boolean isNomeInvalido(CursoModel cursoModel) {
        return cursoModel.getNome() == null || cursoModel.getNome().trim().isEmpty();
    }
}