package br.com.lumilivre.api.service.infra;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import br.com.lumilivre.api.dto.common.AddressLookupResponse;

@Service
public class CepService {

    public AddressLookupResponse buscarEnderecoPorCep(String cep) {
        String url = "https://viacep.com.br/ws/" + cep + "/json/";
        RestTemplate restTemplate = new RestTemplate();
        return restTemplate.getForObject(url, AddressLookupResponse.class);
    }
}
