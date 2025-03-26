package br.com.lumilivre.api.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.lumilivre.api.model.CursoModel;
import br.com.lumilivre.api.model.ResponseModel;
import br.com.lumilivre.api.service.CursoService;

@RestController
@RequestMapping("/lumilivre/cursos")
@CrossOrigin(origins = "*", maxAge = 3600, allowCredentials = "false")
public class CursoController {

	
	@Autowired
	private CursoService cs;
	
	@DeleteMapping("remover/{codigo}")
	public  ResponseEntity<ResponseModel> remover(@PathVariable Long codigo){
		return cs.delete(codigo);
	}
	
	@PutMapping("alterar/{codigo}")
	public ResponseEntity<?> alterar(@PathVariable Long codigo, @RequestBody CursoModel cm){
		cm.setCodigo(codigo);
		return cs.cadastrarAlterar(cm, "alterar");
	}
	
	@PostMapping("/cadastrar")
	public ResponseEntity<?> cadastrar (@RequestBody CursoModel cm){
		return cs.cadastrarAlterar(cm, "cadastrar");
	}
	
	@GetMapping("/listar")
	public Iterable<CursoModel> listar(){
		return cs.listar();
	}
}
