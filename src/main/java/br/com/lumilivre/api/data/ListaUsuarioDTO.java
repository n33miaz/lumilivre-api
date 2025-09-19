package br.com.lumilivre.api.data;

import br.com.lumilivre.api.enums.Role;

public class ListaUsuarioDTO {

	private Integer id;
	private String email;
	private Role role;

	public ListaUsuarioDTO(Integer id, String email, Role role) {
		this.id = id;
		this.email = email;
		this.role = role;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public Role getRole() {
		return role;
	}

	public void setRole(Role role) {
		this.role = role;
	}

}
