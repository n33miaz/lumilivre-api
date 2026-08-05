package br.com.lumilivre.api.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import br.com.lumilivre.api.enums.AccessEvent;

/**
 * Marca um endpoint cujo <b>uso</b> entra na trilha de acessos
 * ({@code access_log}), e não apenas a autenticação.
 *
 * <h2>O critério — e por que ele é restritivo</h2>
 *
 * O erro clássico aqui é anotar tudo: uma linha por requisição não é auditoria,
 * é log de servidor gravado no banco de negócio. Ele cresce sem limite, e o
 * relatório que a coordenação abre fica ilegível justamente porque registrou
 * tudo. Um evento só é admitido quando as três condições valem:
 *
 * <ol>
 *   <li><b>Há uma pessoa identificada.</b> Uso anônimo não gera linha (ver
 *       {@code AccessLogService.recordUsage}): auditoria sem ator não responde
 *       nada, e daria a um chamador anônimo uma escrita no banco por requisição
 *       — exatamente o que não se quer nos endpoints públicos.</li>
 *   <li><b>A pergunta exige o indivíduo.</b> Se a pergunta da coordenação é
 *       "quantos?", a resposta é métrica Prometheus. A linha só se paga quando a
 *       pergunta é "<i>quem</i>, quando, e em qual item": "quantos alunos usaram
 *       o acervo esta semana e quais turmas não usaram", "quais livros os alunos
 *       abrem e a biblioteca não tem exemplar", "quem abriu este comunicado".</li>
 *   <li><b>Não existe outra fonte.</b> Toda escrita de negócio (empréstimo,
 *       solicitação, reserva, cadastro) já vai para {@code audit_log} via
 *       {@link Auditable}, com ator, alvo e IP. Registrar de novo aqui criaria
 *       duas fontes de verdade para o mesmo fato — e elas divergem.</li>
 * </ol>
 *
 * <h2>O que ficou fora, de propósito</h2>
 *
 * <ul>
 *   <li><b>Listagens administrativas</b> ({@code /api/books/search},
 *       {@code /grouped}, {@code /advanced}): é o trabalho do bibliotecário, não
 *       "o aluno usou o sistema". Auditar isso é registrar que alguém estava
 *       trabalhando.</li>
 *   <li><b>Feed do mural</b> ({@code /api/contents/feed}): o app recarrega ao
 *       voltar do background. O sinal útil é qual comunicado foi <i>aberto</i>,
 *       que é {@link AccessEvent#CONTENT_VIEWED}.</li>
 *   <li><b>Download de anexo</b>: não há endpoint. O {@code docUrl} aponta
 *       direto para o storage, então o download não passa pela API e não há onde
 *       registrar sem criar um proxy — fora do escopo desta tarefa.</li>
 *   <li><b>Paginação e refresh</b>: absorvidos pela janela de deduplicação.</li>
 * </ul>
 *
 * @param event evento gravado em {@code access_log.event}
 * @param targetParam expressão Spring EL do recurso consultado (ex.: {@code "#id"}),
 *                    gravada em {@code access_log.target_id}. Vazio quando o
 *                    evento não tem alvo (consulta ao acervo em geral).
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AccessAudited {
    AccessEvent event();

    String targetParam() default "";
}
