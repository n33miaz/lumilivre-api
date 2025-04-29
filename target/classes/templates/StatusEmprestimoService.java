package br.com.lumilivre.api.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import br.com.lumilivre.api.model.StatusEmprestimoModel;
import br.com.lumilivre.api.repository.StatusEmprestimoRepository;

@Service
public class StatusEmprestimoService {
    

    @Autowired
    private StatusEmprestimoRepository ser;
    
    public Iterable<StatusEmprestimoModel> listar(){
    	return ser.findAll();
    
    }
}
