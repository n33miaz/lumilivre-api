package br.com.lumilivre.api.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.lumilivre.api.dto.ListaCursoDTO; // <-- Importação necessária
import br.com.lumilivre.api.model.CursoModel;
import br.com.lumilivre.api.model.ResponseModel;
import br.com.lumilivre.api.repository.CursoRepository;

@Service
public class CursoService {

    @Autowired
    private CursoRepository cr;

    public Page<ListaCursoDTO> buscarCursoParaListaAdmin(String texto, Pageable pageable) {
        return cr.findCursoParaListaAdminComFiltro(texto, pageable);
    }

    public Page<ListaCursoDTO> buscarPorTexto(String texto, Pageable pageable) {
        return cr.buscarPorTextoComDTO(texto, pageable);
    }

    public Page<ListaCursoDTO> buscarAvancado(String nome, Pageable pageable) {
        String nomeFiltro = (nome != null && !nome.isBlank()) ? "%" + nome + "%" : null;
        return cr.buscarAvancadoComDTO(nomeFiltro, pageable);
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