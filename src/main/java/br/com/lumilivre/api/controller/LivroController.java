package br.com.lumilivre.api.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import br.com.lumilivre.api.model.LivroModel;
import br.com.lumilivre.api.model.ResponseModel;
import br.com.lumilivre.api.service.LivroService;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;


@RestController
@CrossOrigin(origins = "*")
public class LivroController {
    
	
	@Autowired
	private LivroService ls;


	@DeleteMapping("/remover/{isbn}")
	public ResponseEntity<ResponseModel> remover(@PathVariable String isbn){
		return ls.delete(isbn);
	}

	@PutMapping("/alterar{id}")
	public ResponseEntity<?> alterar (@RequestBody LivroModel lm){
		return ls.cadastrarAlterar(lm, "alterar");
	}

	@PostMapping("/cadastrar")
	public ResponseEntity<?> cadastrar (@RequestBody LivroModel lm){
		return ls.cadastrarAlterar(lm, "cadastrar");
	}

	// ROTA PARA LISTAR
	@GetMapping("/listar")
	public Iterable<LivroModel> listar (){
		return ls.listar();
	}	
	
	@GetMapping("/")
	public String rota() {
        return "Api funcionando";
	}

}

