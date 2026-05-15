package br.com.lumilivre.api.dto.v1.emprestimo;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonFormat;

import br.com.lumilivre.api.enums.LoanStatus;
import br.com.lumilivre.api.enums.PenaltyCode;
import br.com.lumilivre.api.model.Loan;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmprestimoResponse {

	private UUID id;

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
	private OffsetDateTime dataEmprestimo;

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
	private OffsetDateTime dataDevolucao;

	private LoanStatus status;
	private PenaltyCode penalidade;

	private UUID livroId;
	private String livroTitulo;
	private String imagemUrl;

	private String alunoNome;
	private String alunoMatricula;
	private String exemplarTombo;

	public EmprestimoResponse(
			UUID id,
			OffsetDateTime dataEmprestimo,
			OffsetDateTime dataDevolucao,
			LoanStatus status,
			PenaltyCode penalidade,
			UUID livroId,
			String livroTitulo,
			String imagemUrl) {
		this.id = id;
		this.dataEmprestimo = dataEmprestimo;
		this.dataDevolucao = dataDevolucao;
		this.status = status;
		this.penalidade = penalidade;
		this.livroId = livroId;
		this.livroTitulo = livroTitulo;
		this.imagemUrl = imagemUrl;
	}

	public EmprestimoResponse(Loan loan) {
		this.id = loan.getId();
		this.dataEmprestimo = loan.getBorrowedAt();
		this.dataDevolucao = loan.getDueAt();
		this.status = loan.getStatus();
		this.penalidade = loan.getPenaltyCode();

		if (loan.getBookCopy() != null) {
			this.exemplarTombo = loan.getBookCopy().getCopyCode();

			if (loan.getBookCopy().getBook() != null) {
				this.livroTitulo = loan.getBookCopy().getBook().getTitle();
				this.livroId = loan.getBookCopy().getBook().getId();
				this.imagemUrl = loan.getBookCopy().getBook().getCoverUrl();
			}
		}

		if (loan.getStudent() != null) {
			this.alunoNome = loan.getStudent().getFullName();
			this.alunoMatricula = loan.getStudent().getRegistrationNumber();
		}
	}
}
