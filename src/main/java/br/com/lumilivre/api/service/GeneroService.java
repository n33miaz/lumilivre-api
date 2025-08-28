package br.com.lumilivre.api.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.lumilivre.api.data.ListaEmprestimoDTO;
import br.com.lumilivre.api.data.ListaGeneroDTO;
import br.com.lumilivre.api.enums.Role;
import br.com.lumilivre.api.model.GeneroModel;
import br.com.lumilivre.api.model.ResponseModel;
import br.com.lumilivre.api.model.UsuarioModel;
import br.com.lumilivre.api.repository.GeneroRepository;

@Service
public class GeneroService {

    @Autowired
    private GeneroRepository gr;
    
    public Page<ListaGeneroDTO> buscarGeneroParaListaAdmin(Pageable pageable) {
        return gr.findGeneroParaListaAdmin(pageable);
    }
    
    public Page<GeneroModel> buscarPorTexto(String texto, Pageable pageable) {
    if (texto == null || texto.isBlank()) {
        return gr.findAll(pageable);
    }
    return gr.buscarPorTexto(texto, pageable); 
}
    public Page<GeneroModel> buscarAvancado(
        Integer id,
        String nome,
        Pageable pageable) {
    return gr.buscarAvancado(id, nome, pageable);
}



    @Transactional
    public ResponseEntity<?> cadastrar(GeneroModel generoModel) {
        if (isNomeInvalido(generoModel)) {
            ResponseModel rm = new ResponseModel();
            rm.setMensagem("O Nome é Obrigatório");
            return ResponseEntity.badRequest().body(rm);
        }

        if (gr.existsByNomeIgnoreCaseAndIdNot(generoModel.getNome(),
                generoModel.getId())) {
            ResponseModel rm = new ResponseModel();
            rm.setMensagem("O Nome já existe no banco de dados");
            return ResponseEntity.badRequest().body(rm);
        }
        GeneroModel salvo = gr.save(generoModel);
        return ResponseEntity.status(HttpStatus.CREATED).body(salvo);
    }

    @Transactional
    public ResponseEntity<?> atualizar(GeneroModel generoModel) {
        if (isNomeInvalido(generoModel)) {
            ResponseModel rm = new ResponseModel();
            rm.setMensagem("O Nome é Obrigatório");
            return ResponseEntity.badRequest().body(rm);
        }
        if (gr.existsByNomeIgnoreCaseAndIdNot(generoModel.getNome(),
                generoModel.getId())) {
            ResponseModel rm = new ResponseModel();
            rm.setMensagem("O Nome já existe no banco de dados");
            return ResponseEntity.badRequest().body(rm);
        }
        GeneroModel salvo = gr.save(generoModel);
        return ResponseEntity.ok(salvo);
    }

    @Transactional
    public ResponseEntity<ResponseModel> excluir(Integer id) {
        gr.deleteById(id);
        ResponseModel rm = new ResponseModel();
        rm.setMensagem("O Genero foi removido com sucesso");
        return new ResponseEntity<ResponseModel>(rm, HttpStatus.OK);
    }

    private boolean isNomeInvalido(GeneroModel generoModel) {
        return generoModel.getNome() == null || generoModel.getNome().trim().isEmpty();
    }

    public GeneroModel buscarPorNome(String nome) {
        return gr.findByNomeIgnoreCase(nome);
    }

}