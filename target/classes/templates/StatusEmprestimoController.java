package br.com.lumilivre.api.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import br.com.lumilivre.api.model.StatusEmprestimoModel;
import br.com.lumilivre.api.service.StatusEmprestimoService;

@RestController
@RequestMapping("/lumilivre/status_emprestimo")
@CrossOrigin(origins = "*", maxAge = 3600, allowCredentials = "false")
public class StatusEmprestimoController {

	@Autowired
	private StatusEmprestimoService ses;
	
	@GetMapping("/listar")
	public Iterable<StatusEmprestimoModel> listar(){
		return ses.listar();
		
		
	}
}
