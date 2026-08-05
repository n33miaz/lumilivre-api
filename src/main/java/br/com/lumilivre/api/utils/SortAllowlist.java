package br.com.lumilivre.api.utils;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import br.com.lumilivre.api.exception.custom.BusinessRuleException;

/**
 * Traduz o {@code sort} que vem do request para colunas conhecidas antes de a
 * ordenacao chegar na query.
 *
 * <p>Existe por causa da query nativa: numa query nativa o Spring Data
 * interpola o {@code ORDER BY} do {@link Sort} como <b>texto</b>, sem
 * {@code PreparedStatement} nenhum no caminho. Foi confirmado no stack local
 * que {@code ?sort=copy_code} e {@code ?sort=l.title} ordenam de verdade (nome
 * de coluna e alias cru do SQL, nao propriedade da API) e que
 * {@code ?sort=coluna_inexistente} chega no Postgres — ou seja, o texto do
 * cliente entra na consulta. O guarda do proprio Spring Data recusa pontuacao,
 * mas isso e detalhe de implementacao dele e ainda deixa passar enumeracao de
 * colunas do schema, respondendo 500.
 *
 * <p>Com a allowlist, campo fora do mapa vira 400 com mensagem clara e nada de
 * texto do cliente alcanca a consulta: o que segue e sempre uma constante
 * escrita aqui.
 */
public final class SortAllowlist {

    private final Map<String, String> columnsByField;

    private SortAllowlist(LinkedHashMap<String, String> columnsByField) {
        // unmodifiableMap e nao Map.copyOf: o copyOf devolve mapa cuja ordem de
        // iteracao e salteada por JVM, o que quebraria a "ordem de declaracao"
        // que allowedFields() promete — e faria a mensagem de erro listar os
        // campos aceitos em ordem diferente a cada reinicio. O mapa vem do
        // of(), que o cria e nunca o compartilha, entao embrulhar basta.
        this.columnsByField = Collections.unmodifiableMap(columnsByField);
    }

    /**
     * @param pairs nome logico da API seguido da coluna real, alternados
     */
    public static SortAllowlist of(String... pairs) {
        if (pairs.length % 2 != 0) {
            throw new IllegalArgumentException("SortAllowlist.of expects field/column pairs");
        }
        LinkedHashMap<String, String> map = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            map.put(pairs[i], pairs[i + 1]);
        }
        return new SortAllowlist(map);
    }

    /** Campos aceitos, na ordem de declaracao — usado nas mensagens de erro. */
    public List<String> allowedFields() {
        return List.copyOf(columnsByField.keySet());
    }

    /**
     * Devolve o mesmo {@link Pageable} com o sort reescrito em colunas da
     * allowlist, mantendo a direcao pedida.
     *
     * @throws BusinessRuleException (400) quando o cliente pede um campo que
     *         nao esta na allowlist
     */
    public Pageable sanitize(Pageable pageable) {
        if (pageable == null || pageable.getSort().isUnsorted()) {
            return pageable;
        }

        List<Sort.Order> safeOrders = pageable.getSort().stream()
                .map(this::toSafeOrder)
                .toList();

        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(safeOrders));
    }

    private Sort.Order toSafeOrder(Sort.Order requested) {
        String column = columnsByField.get(requested.getProperty());
        if (column == null) {
            throw BusinessRuleException.ofKey("error.sort.invalid-field",
                    requested.getProperty(), String.join(", ", allowedFields()));
        }
        return new Sort.Order(requested.getDirection(), column, requested.getNullHandling());
    }
}
