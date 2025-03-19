package br.com.lumilivre.api.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.lumilivre.api.model.LivroModel;
import br.com.lumilivre.api.service.LivroService;

@RestController
public class LivroController {
    
    // rota teste
	
	@Autowired
	private LivroService ls;
	
	@GetMapping("/Listar")
	public Iterable<LivroModel> listar (){
		return ls.listar();
		}	
	
	@GetMapping("/")
	public String rota() {
        return "Api funcionando";
	}

}

