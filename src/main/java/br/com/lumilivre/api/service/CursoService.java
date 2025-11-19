package br.com.lumilivre.api.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.lumilivre.api.dto.curso.CursoRequest;
import br.com.lumilivre.api.dto.curso.CursoResponse;
import br.com.lumilivre.api.dto.curso.CursoResumoResponse;
import br.com.lumilivre.api.exception.custom.RecursoNaoEncontradoException;
import br.com.lumilivre.api.model.CursoModel;
import br.com.lumilivre.api.model.ResponseModel;
import br.com.lumilivre.api.repository.CursoRepository;

@Service
public class CursoService {

    @Autowired
    private CursoRepository cr;

    public Page<CursoResumoResponse> buscarCursoParaListaAdmin(String texto, Pageable pageable) {
        return cr.findCursoParaListaAdminComFiltro(texto, pageable);
    }

    public Page<CursoResumoResponse> buscarPorTexto(String texto, Pageable pageable) {
        return cr.buscarPorTextoComDTO(texto, pageable);
    }

    public Page<CursoResumoResponse> buscarAvancado(String nome, Pageable pageable) {
        String nomeFiltro = (nome != null && !nome.isBlank()) ? "%" + nome + "%" : null;
        return cr.buscarAvancadoComDTO(nomeFiltro, pageable);
    }

    @Transactional
    public ResponseEntity<CursoResponse> cadastrar(CursoRequest dto) {
        CursoModel curso = new CursoModel();
        curso.setNome(dto.getNome());

        CursoModel salvo = cr.save(curso);

        return ResponseEntity.status(HttpStatus.CREATED).body(new CursoResponse(salvo));
    }

    @Transactional
    public ResponseEntity<CursoResponse> atualizar(Integer id, CursoRequest dto) {
        CursoModel curso = cr.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Curso não encontrado com ID: " + id));

        curso.setNome(dto.getNome());

        CursoModel salvo = cr.save(curso);

        return ResponseEntity.ok(new CursoResponse(salvo));
    }

    @Transactional
    public ResponseEntity<ResponseModel> excluir(Integer id) {
        if (!cr.existsById(id)) {
            throw new RecursoNaoEncontradoException("Curso não encontrado com ID: " + id);
        }

        cr.deleteById(id);

        ResponseModel rm = new ResponseModel();
        rm.setMensagem("O Curso foi removido com sucesso");
        return new ResponseEntity<>(rm, HttpStatus.OK);
    }
}