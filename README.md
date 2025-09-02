# Lumi Livre - API Backend

API RESTful desenvolvida em Java com Spring Boot para o sistema de gerenciamento bibliotecário Lumi Livre. Este backend é responsável por toda a lógica de negócio, gerenciamento de dados e segurança da aplicação.

## Visão Geral da Tecnologia

- **Linguagem**: Java 17
- **Framework**: Spring Boot 3.2.5
- **Banco de Dados**: PostgreSQL (com Spring Data JPA & Hibernate)
- **Segurança**: Spring Security com autenticação via JWT (JSON Web Tokens)
- **Build Tool**: Apache Maven
- **Documentação**: Swagger / OpenAPI 3 (integrado com Springdoc)

---

## 🚀 Como Rodar o Projeto Localmente

Siga estes passos para configurar e executar o backend na sua máquina.

### 1. Pré-requisitos

Garanta que você tenha as seguintes ferramentas instaladas:

| Ferramenta      | Versão Mínima | Instalação (Windows - via [Chocolatey](https://chocolatey.org/))                               | Instalação (Linux - via apt/dnf)                                          |
| --------------- | ------------- | ----------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------- |
| **JDK (Java)**  | `17`          | `choco install openjdk --version=17`                                                            | `sudo apt-get install openjdk-17-jdk` ou `sudo dnf install java-17-openjdk` |
| **Git**         | `2.x`         | `choco install git`                                                                             | `sudo apt-get install git` ou `sudo dnf install git`                        |
| **Maven**       | `3.8+`        | `choco install maven` (Opcional, pois o projeto usa o Maven Wrapper)                              | `sudo apt-get install maven` ou `sudo dnf install maven`                    |

**Verificação:**
Após a instalação, abra um novo terminal e verifique as versões com:
```bash
java -version
git --version
```

### 2. Configuração do Banco de Dados

Esta API se conecta a um banco de dados PostgreSQL. As credenciais de acesso **não estão** no repositório por segurança.

1.  Na pasta `src/main/resources/`, encontre o arquivo `application.properties.example`.
2.  Crie uma cópia deste arquivo no mesmo diretório e renomeie-a para `application.properties`.
3.  Abra o `application.properties` e preencha as seguintes propriedades com as credenciais corretas do banco de dados (fornecidas pela equipe):
    ```properties
    spring.datasource.url=jdbc:postgresql://<host>:<port>/<database>
    spring.datasource.username=<seu_usuario>
    spring.datasource.password=<sua_senha>
    ```

### 3. Clonando e Construindo o Projeto

1.  **Clone o repositório:**
    ```bash
    git clone git@github.com:TCC-DS-2025/lumilivre.git
    cd lumilivre
    ```

2.  **Resolva a permissão do Git (Apenas para Windows, se necessário):**
    Se o Git reclamar de "dubious ownership", execute o comando sugerido por ele, colocando o caminho entre aspas:
    ```bash
    git config --global --add safe.directory "<caminho_completo_para_a_pasta_lumilivre>"
    ```

3.  **Construa o projeto com o Maven Wrapper:**
    O wrapper (`mvnw`) irá baixar a versão correta do Maven automaticamente.
    
    *   **Para Linux/macOS:**
        ```bash
        ./mvnw clean install
        ```
    *   **Para Windows (CMD/PowerShell):**
        ```bash
        .\mvnw.cmd clean install
        ```
        *(No Git Bash para Windows, use `./mvnw clean install`)*

    Este comando irá baixar todas as dependências e compilar o projeto. A primeira execução pode demorar alguns minutos.

### 4. Executando a Aplicação

Com o projeto construído, inicie o servidor Spring Boot:

*   **Para Linux/macOS:**
    ```bash
    ./mvnw spring-boot:run
    ```
*   **Para Windows:**
    ```bash
    .\mvnw.cmd spring-boot:run
    ```

O servidor iniciará e estará disponível na porta **8080**. Você verá a mensagem `Tomcat started on port(s): 8080 (http)` no seu console.

---

## 📚 Acessando a Documentação da API (Swagger)

Com a aplicação rodando, a documentação interativa da API está disponível. Ela é a melhor ferramenta para entender e testar todos os endpoints.

1.  Abra seu navegador.
2.  Acesse a URL: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

### Testando Endpoints Protegidos:
1.  Use o endpoint `POST /auth/login` para obter um token JWT.
2.  Clique no botão **"Authorize"** no canto superior direito da página do Swagger.
3.  Na janela que abrir, cole o token JWT que você recebeu (sem o "Bearer ").
4.  Agora você pode executar qualquer endpoint protegido que exija autenticação.