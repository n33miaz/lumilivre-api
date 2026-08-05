package br.com.lumilivre.api.security;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import br.com.lumilivre.api.model.AuditLog;
import br.com.lumilivre.api.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;

/**
 * Escrita de {@link AuditLog} em transação própria.
 *
 * <p>Existe por um defeito concreto: o {@link AuditAspect} grava dentro da
 * transação do método auditado, então a linha de <b>FAILURE</b> nascia marcada
 * para rollback junto com a regra de negócio que falhou e desaparecia sem erro
 * nenhum. Ou seja, a trilha guardava só o que deu certo — e o que uma revisão de
 * segurança procura é justamente a tentativa que não deu.
 *
 * <p>{@code REQUIRES_NEW} suspende a transação de negócio e comita a linha de
 * auditoria sozinha. O custo é uma segunda conexão do pool durante o insert, e
 * ele só é pago no caminho de falha — o de sucesso continua na transação do
 * chamador, de propósito: ali o registro <i>deve</i> ser atômico com a mudança,
 * senão a trilha passaria a afirmar coisas que não aconteceram quando o commit
 * do negócio falhasse no flush.
 */
@Component
@RequiredArgsConstructor
public class AuditLogWriter {

    private final AuditLogRepository auditLogRepository;

    /**
     * Sucesso: participa da transação do chamador (atômico com a mudança).
     * {@code REQUIRED} e não {@code MANDATORY} para que um método auditado sem
     * transação própria continue gerando linha em vez de estourar.
     */
    @Transactional
    public void writeInCallerTransaction(AuditLog auditLog) {
        auditLogRepository.save(auditLog);
    }

    /** Falha: transação própria, porque a do chamador está condenada. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void writeInNewTransaction(AuditLog auditLog) {
        auditLogRepository.save(auditLog);
    }
}
