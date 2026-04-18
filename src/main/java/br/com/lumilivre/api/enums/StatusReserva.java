package br.com.lumilivre.api.enums;

public enum StatusReserva {
    /** Aguardando na fila — livro ainda emprestado */
    AGUARDANDO,
    /** Exemplar disponível, notificação enviada, aguardando retirada */
    DISPONIVEL_PARA_RETIRADA,
    /** Empréstimo efetivado — reserva finalizada com sucesso */
    CONVERTIDA,
    /** Cancelada pelo aluno ou expirada por tempo limite */
    CANCELADA,
    /** Expirada: aluno não retirou dentro do prazo após notificação */
    EXPIRADA
}
