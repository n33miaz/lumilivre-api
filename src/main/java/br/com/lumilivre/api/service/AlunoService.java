package br.com.lumilivre.api.service;

import javax.smartcardio.ResponseAPDU;

import org.apache.catalina.connector.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import br.com.lumilivre.api.model.AlunoModel;
import br.com.lumilivre.api.model.ResponseModel;
import br.com.lumilivre.api.repository.AlunoRepository;

@Service
public class AlunoService {
    
    @Autowired
    private AlunoRepository ar;

    @Autowired
    private ResponseModel rm;

    public Iterable<AlunoModel> listar(){
        return ar.findAll();
    }

    public ResponseEntity<?> cadastrarAlterar(AlunoModel am, String acao){
        if(am.getNome().equals("")){
            rm.setMensagem("o Nome do Aluno é obrigatório");
            return new ResponseEntity<ResponseModel>(rm, HttpStatus.BAD_REQUEST);
        } else if (am.getMatricula().equals("")){
            rm.setMensagem("A matrícula do Aluno é obrigatória");
            return new ResponseEntity<ResponseModel>(rm, HttpStatus.BAD_REQUEST);
        } else if (am.getCpf().equals("")){
            rm.setMensagem("o CPF do Aluno é obrigatório");
            return new ResponseEntity<ResponseModel>(rm, HttpStatus.BAD_REQUEST);
        } else {
            if(acao.equals("cadastrar")){
                return new ResponseEntity<AlunoModel>(ar.save(am), HttpStatus.CREATED);
            } else {
                return new ResponseEntity<AlunoModel>(ar.save(am), HttpStatus.OK);
            }
        }
                
    }

    public ResponseEntity<ResponseModel> delete (String matricula){
        ar.deleteById(matricula);
        rm.setMensagem("O aluno foi removido com sucesso");
        return new ResponseEntity<ResponseModel>(rm, HttpStatus.OK);
    }
}

