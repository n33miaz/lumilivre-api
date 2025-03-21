package br.com.lumilivre.api.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.lumilivre.api.model.LivroModel;
import br.com.lumilivre.api.model.ResponseModel;
import br.com.lumilivre.api.service.LivroService;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;

@RestController
@RequestMapping("/lumilivre/livros")
@CrossOrigin(origins = "*", maxAge = 3600, allowCredentials = "false")
public class LivroController {
    
	@Autowired
	private LivroService ls;


	@DeleteMapping("/remover/{isbn}")
	public ResponseEntity<ResponseModel> remover(@PathVariable String isbn){
		return ls.delete(isbn);
	}

	@PutMapping("/alterar/{isbn}")
	public ResponseEntity<?> alterar(@PathVariable String isbn, @RequestBody LivroModel lm) {
		lm.setIsbn(isbn); // Garante que o ISBN do objeto seja atualizado corretamente
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

