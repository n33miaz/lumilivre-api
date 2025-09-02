package br.com.lumilivre.api.model;

import org.springframework.stereotype.Component;

@Component
public class ResponseModel {

	private String mensagem;

	public String getMensagem() {
		return mensagem;
	}

	public void setMensagem(String mensagem) {
		this.mensagem = mensagem;
	}

}
