package br.com.lumilivre.api.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import br.com.lumilivre.api.dto.aluno.AlunoRequest;

@Service
public class CepService {

    public AlunoRequest buscarEnderecoPorCep(String cep) {
        String url = "https://viacep.com.br/ws/" + cep + "/json/";
        RestTemplate restTemplate = new RestTemplate();
        return restTemplate.getForObject(url, AlunoRequest.class);
    }
}
