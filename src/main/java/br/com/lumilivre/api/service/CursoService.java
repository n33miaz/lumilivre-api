package br.com.lumilivre.api.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import br.com.lumilivre.api.data.ListaCursoDTO;
import br.com.lumilivre.api.model.CursoModel;
import br.com.lumilivre.api.model.ResponseModel;
import br.com.lumilivre.api.repository.CursoRepository;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CursoService {

    @Autowired
    private CursoRepository cr;

    public Page<ListaCursoDTO> buscarCursoParaListaAdmin(Pageable pageable) {
        return cr.findCursoParaListaAdmin(pageable);
    }
    
    
    public Page<CursoModel> buscarPorTexto(String texto, Pageable pageable) {
        if (texto == null || texto.isBlank()) {
            return cr.findAll(pageable);
        }
        return cr.buscarPorTexto(texto, pageable);
    }

    public Page<CursoModel> buscarAvancado(
            String nome,
            String turno,
            String modulo,

            Pageable pageable) {
        return cr.buscarAvancado(nome, turno, modulo, pageable);
    }

    @Transactional
    public ResponseEntity<?> cadastrar(CursoModel cursoModel) {
        if (isNomeInvalido(cursoModel)) {
            ResponseModel rm = new ResponseModel();
            rm.setMensagem("O Nome é Obrigatório");
            return ResponseEntity.badRequest().body(rm);
        }

        CursoModel salvo = cr.save(cursoModel);
        return ResponseEntity.status(HttpStatus.CREATED).body(salvo);
    }

    @Transactional
    public ResponseEntity<?> atualizar(CursoModel cursoModel) {
        if (isNomeInvalido(cursoModel)) {
            ResponseModel rm = new ResponseModel();
            rm.setMensagem("O Nome é Obrigatório");
            return ResponseEntity.badRequest().body(rm);
        }

        CursoModel salvo = cr.save(cursoModel);
        return ResponseEntity.ok(salvo);
    }

    @Transactional
    public ResponseEntity<ResponseModel> excluir(Integer id) {
        cr.deleteById(id);
        ResponseModel rm = new ResponseModel();
        rm.setMensagem("O Curso foi removido com sucesso");
        return new ResponseEntity<>(rm, HttpStatus.OK);
    }

    private boolean isNomeInvalido(CursoModel cursoModel) {
        return cursoModel.getNome() == null || cursoModel.getNome().trim().isEmpty();
    }

}