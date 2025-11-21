package br.com.lumilivre.api.dto.emprestimo;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import br.com.lumilivre.api.enums.Penalidade;
import br.com.lumilivre.api.enums.StatusEmprestimo;
import br.com.lumilivre.api.model.EmprestimoModel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmprestimoResponse {

	private Integer id;

	@JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss")
	private LocalDateTime dataEmprestimo;

	@JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss")
	private LocalDateTime dataDevolucao;

	private StatusEmprestimo status;
	private Penalidade penalidade;
	private String livroTitulo;

	private String alunoNome;
	private String alunoMatricula;
	private String exemplarTombo;

	public EmprestimoResponse(EmprestimoModel model) {
		this.id = model.getId();
		this.dataEmprestimo = model.getDataEmprestimo();
		this.dataDevolucao = model.getDataDevolucao();
		this.status = model.getStatusEmprestimo();
		this.penalidade = model.getPenalidade();

		if (model.getExemplar() != null) {
			this.exemplarTombo = model.getExemplar().getTombo();
			if (model.getExemplar().getLivro() != null) {
				this.livroTitulo = model.getExemplar().getLivro().getNome();
			}
		}

		if (model.getAluno() != null) {
			this.alunoNome = model.getAluno().getNomeCompleto();
			this.alunoMatricula = model.getAluno().getMatricula();
		}
	}
}