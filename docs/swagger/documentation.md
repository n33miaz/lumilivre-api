# documentação swagger/openapi

## convenções

Este documento é a fonte de verdade para a documentação da API LumiLivre.

### princípios

- Controllers declaram apenas identificadores estáveis de máquina: `@Tag(name = SwaggerTags.BOOKS)` e `@Operation(operationId = "books.list")`.
- Textos visíveis para usuários ficam em `src/main/resources/i18n/swagger/*_{pt_BR,en_US}.properties`.
- A mesma operação deve renderizar corretamente nos grupos `api-pt-br` e `api-en-us`.
- Endpoints públicos não devem mostrar o cadeado de Bearer token. Endpoints protegidos devem mostrar autenticação Bearer.
- Novas tags devem ser adicionadas em `SwaggerTags.ORDERED` e nos bundles localizados.

### ids de operação

Use `{tagKey}.{verb}`:

| caso de uso HTTP | sufixos recomendados |
|---|---|
| listagem/busca | `list`, `search`, `advanced`, `byStudent` |
| leitura única | `get`, `isbnLookup`, `postalCode` |
| criação/atualização/remoção | `create`, `update`, `delete` |
| ações | `close`, `renew`, `cancel`, `process` |
| uploads/importações | `uploadCover`, `uploadAvatar`, `students`, `books`, `copies` |

Exemplos: `auth.login`, `books.catalog`, `loans.close`, `loan-requests.process`.

### chaves dos bundles

Chaves mínimas por operação:

```properties
swagger.operation.{operationId}.summary=Rótulo curto da ação
swagger.operation.{operationId}.description=Propósito de negócio, permissões e regras relevantes.
```

Chaves opcionais:

```properties
swagger.parameter.{operationId}.{paramName}.description=Texto específico do parâmetro.
swagger.requestBody.{operationId}.description=Propósito do corpo da requisição.
swagger.response.{operationId}.{status}.description=Texto específico da resposta.
swagger.schema.{SchemaName}.description=Propósito do schema.
swagger.schema.{SchemaName}.{field}.description=Propósito do campo.
```

Chaves compartilhadas de fallback ficam em `_common_{locale}.properties`.

## contribuição

Ao adicionar ou alterar um endpoint:

1. Adicione ou atualize `@Tag(name = SwaggerTags.X)` no controller.
2. Adicione `@Operation(operationId = "{tag}.{verb}")` em todo método com mapping.
3. Adicione `swagger.operation.{operationId}.summary` e `.description` em PT-BR.
4. Adicione exatamente as mesmas chaves em EN-US.
5. Adicione chaves de request body e parâmetros quando o texto gerado não for suficiente.
6. Rode:

```bash
bash scripts/check-i18n-coverage.sh
bash scripts/check-openapi-annotations.sh
./mvnw test
```

Não coloque textos visíveis para usuários diretamente nas annotations. O código Java deve permanecer estável quando apenas textos ou traduções mudarem.

## adicionando outro idioma no swagger

Exemplo para espanhol (`es-ES`):

1. Copie cada arquivo `src/main/resources/i18n/swagger/*_pt_BR.properties` para `*_es_ES.properties`.
2. Traduza apenas os valores. Mantenha as chaves inalteradas.
3. Adicione o locale em `I18nConfig.localeResolver().setSupportedLocales(...)`.
4. Adicione um novo grupo em `OpenApiConfig`:

```java
@Bean
public GroupedOpenApi apiEsEsGroup() {
    return localizedGroup("api-es-es", "API - Espanol (Espana)", Locale.forLanguageTag("es-ES"));
}
```

5. Rode o script de cobertura e os testes.

Adicionar um idioma não deve exigir mudanças nos controllers nem nos operation IDs.

## glossário

| PT-BR | EN-US | observações |
|---|---|---|
| Empréstimo | Loan | Saída física de um exemplar para um aluno. |
| Exemplar | Book copy | Unidade física do acervo identificada por tombo. |
| Aluno | Student | Usuário da biblioteca com matrícula. |
| Bibliotecário | Librarian | Perfil operacional da equipe. |
| Usuário administrativo | Admin user | Conta ADMIN ou LIBRARIAN. |
| Solicitação | Loan request | Pedido que pode virar empréstimo quando aceito. |
| Reserva | Reservation | Fila FIFO para um livro. |
| TCC | Thesis | Trabalho acadêmico de conclusão de curso. |
| CDD | Dewey Classification | Em textos em português, manter a sigla `CDD`. |
| Tombo | Copy code | Identificador patrimonial de um exemplar físico. |
| Matrícula | Registration number | Identificador do aluno. |
| CEP | Postal code | Manter `CEP` apenas nos textos PT-BR. |
