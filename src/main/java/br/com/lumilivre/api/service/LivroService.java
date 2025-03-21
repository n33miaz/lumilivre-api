package br.com.lumilivre.api.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import br.com.lumilivre.api.model.LivroModel;
import br.com.lumilivre.api.model.ResponseModel;
import br.com.lumilivre.api.repository.LivroRepository;

@Service
public class LivroService {
	
	@Autowired
	private LivroRepository lr;

	@Autowired
	private ResponseModel rm;
    
	// metodo para listar os livros
	public Iterable<LivroModel> listar(){
		return lr.findAll();	
		}
	
		// Cadastro ou Alteração
	public ResponseEntity<?> cadastrarAlterar(LivroModel lm, String acao){
		
		if (lm.getNome().equals("")) {
			rm.setMensagem("O Nome do Livro é obrigatório");
			return new ResponseEntity<ResponseModel>(rm, HttpStatus.BAD_REQUEST);
		} else if (lm.getIsbn().equals("")){
			rm.setMensagem("O ISBN do Livro é Obrigatório");
			return new ResponseEntity<ResponseModel>(rm, HttpStatus.BAD_REQUEST);
		} else if (lm.getAutor().equals("")) {
			rm.setMensagem("o Autor do Livro é Obrigátorio");
			return new ResponseEntity<ResponseModel>(rm, HttpStatus.BAD_REQUEST);
		} else {
			if(acao.equals("cadastrar")){
				return new ResponseEntity<LivroModel>(lr.save(lm), HttpStatus.CREATED);
			} else {
				return new ResponseEntity<LivroModel>(lr.save(lm), HttpStatus.OK);

			}
		}
		
	}
	

	// REMOVER PRODUTOS 
	public ResponseEntity<ResponseModel> delete(String isbn){
		lr.deleteById(isbn);
		rm.setMensagem("O Livro foi removido com sucesso");
		return new ResponseEntity<ResponseModel>(rm, HttpStatus.OK);
	}
	
}
