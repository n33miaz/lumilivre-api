package br.com.lumilivre.api.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import br.com.lumilivre.api.model.CursoModel;
import br.com.lumilivre.api.model.ResponseModel;
import br.com.lumilivre.api.repository.CursoRepository;

@Service
public class CursoService{
	
	
	@Autowired 
	private CursoRepository cr;
	
	@Autowired
	private ResponseModel rm;
	
	public Iterable<CursoModel> listar(){
		return cr.findAll();
	}
	
	
	public ResponseEntity<?> cadastrarAlterar(CursoModel cm, String acao){
		if (cm.getNome().equals("")) {
			rm.setMensagem("O Nome é Obrigátorio");
			return new ResponseEntity<ResponseModel>(rm, HttpStatus.BAD_REQUEST);
		} else if (cm.getTurno().equals("")) {
			rm.setMensagem("O Turno é Obrigatório");
			return new ResponseEntity<ResponseModel>(rm, HttpStatus.BAD_REQUEST);
		} else {
			if(acao.equals("cadastrar")) {
				return new ResponseEntity<CursoModel>(cr.save(cm), HttpStatus.CREATED);
			} else {
				return new ResponseEntity<CursoModel>(cr.save(cm), HttpStatus.OK);
			}
		}
		
	}
	
	public ResponseEntity<ResponseModel> delete(Long codigo){
		cr.deleteById(codigo);
		rm.setMensagem("O Curso foi removido com sucesso");
		return new ResponseEntity<ResponseModel>(rm, HttpStatus.OK);
	}
	
}