package br.com.lumilivre.api.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import br.com.lumilivre.api.model.LivroModel;
import br.com.lumilivre.api.repository.LivroRepository;

@Service
public class LivroService {
	
	@Autowired
	
	private LivroRepository lr;
    
	// metodo para listar os livros
	public Iterable<LivroModel> listar(){
		return lr.findAll();	
		}
}
