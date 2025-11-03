package br.com.lumilivre.api.model;

public class ResponseModel {

	private String mensagem;

	public ResponseModel() {
	}

	public ResponseModel(String mensagem) {
		this.mensagem = mensagem;
	}

	public String getMensagem() {
		return mensagem;
	}

	public void setMensagem(String mensagem) {
		this.mensagem = mensagem;
	}
}