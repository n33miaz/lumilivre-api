<div align="center">
  <!-- Banner -->
  <a href="https://n33miaz.github.io/n33miaz-links/#lumitcc">
    <img width="100%" src="https://github-stats-api-fmwm.onrender.com/api/banner?title=LumiLivre&subtitle=Library%20Management%20System&tag=(TCC)%20Bachelor%27s%20Thesis&title_color=762075&text_color=c9d1d9&v=1" alt="LumiLivre Banner" />
  </a>

  <!-- Pins dos Repositórios -->
  <a href="https://n33miaz.github.io/n33miaz-links/#lumiweb"><img src="https://github-stats-api-fmwm.onrender.com/api/pin?username=n33miaz&repo=lumilivre-web&custom_title=WebSite&bg_color=0d1117&title_color=762075&text_color=c9d1d9&icon_color=762075&hide_border=true&min_width=270" height="140" /></a>&nbsp;&nbsp;&nbsp;
  <a href="https://n33miaz.github.io/n33miaz-links/#lumiapp"><img src="https://github-stats-api-fmwm.onrender.com/api/pin?username=n33miaz&repo=lumilivre-app&custom_title=Application&bg_color=0d1117&title_color=762075&text_color=c9d1d9&icon_color=762075&hide_border=true&min_width=270" height="140" /></a>&nbsp;&nbsp;&nbsp;
  <a href="https://n33miaz.github.io/n33miaz-links/#lumiapi"><img src="https://github-stats-api-fmwm.onrender.com/api/pin?username=n33miaz&repo=lumilivre-api&custom_title=API%20Restfull&bg_color=0d1117&title_color=762075&text_color=c9d1d9&icon_color=762075&hide_border=true&min_width=270" height="140" /></a>
</div>

<br/>

<div align="center">
  <h1>Sobre o Projeto</h1>
</div>

A **LumiLivre API** é o núcleo de processamento e inteligência de todo o ecossistema. Desenvolvida em **Java 17** com **Spring Boot 3**, ela centraliza a lógica de negócios, a persistência de dados e a segurança, servindo tanto o painel administrativo (Web) quanto o aplicativo dos alunos (Mobile).

Atualmente hospedada no **Render** via Docker, a API utiliza **PostgreSQL** (hospedado no Supabase) como banco de dados relacional, garantindo robustez e integridade para as operações da biblioteca.

A documentação interativa está disponível em: [api-lumilivre.com.br/swagger-ui](https://lumilivre-api.onrender.com/swagger-ui/index.html#/).

<br/>

<div align="center">
  <h1>Funcionalidades Principais</h1>
</div>

### 🧠 Regras de Negócio
- **Gestão de Empréstimos:** Controle rigoroso de prazos, renovações e cálculo automático de multas/penalidades baseadas em dias de atraso.
- **Controle de Estoque:** Gerenciamento de exemplares físicos, status de disponibilidade e baixa automática.
- **Validação de Usuários:** Lógica diferenciada para Administradores, Bibliotecários e Alunos.

### 🔌 Integrações Externas
- **Google Books & BrasilAPI:** Preenchimento automático de metadados de livros (sinopse, autor, capa) apenas informando o ISBN.
- **Supabase Storage:** Armazenamento em nuvem para capas de livros e arquivos PDF de TCCs.
- **Serviço de E-mail:** Notificações automáticas para empréstimos realizados, devoluções e redefinição de senha.

### 📊 Relatórios & Dados
- **Geração de PDF:** Engine interna (OpenPDF) para gerar relatórios detalhados de acervo e movimentações.
- **Dashboards:** Endpoints otimizados para fornecer estatísticas em tempo real para os clientes frontend.
- **Importação em Massa:** Processamento de arquivos Excel (.xlsx) para carga inicial de dados.

<br/>

<div align="center">
  <h1>Arquitetura do Sistema</h1>
</div>

Utilizamos uma arquitetura cliente-servidor moderna baseada em microsserviços e nuvem para garantir escalabilidade.

```mermaid
flowchart TD
    classDef mobile fill:#02569B,stroke:#fff,stroke-width:2px,color:#fff;
    classDef web fill:#61DAFB,stroke:#fff,stroke-width:2px,color:#000;
    classDef api fill:#762075,stroke:#fff,stroke-width:2px,color:#fff;
    classDef db fill:#336791,stroke:#fff,stroke-width:2px,color:#fff;
    classDef storage fill:#3ECF8E,stroke:#fff,stroke-width:2px,color:#fff;
    classDef external fill:#ddd,stroke:#333,stroke-width:1px,color:#000,stroke-dasharray: 5 5;

    UserMobile["Application (Aluno)"]:::mobile
    UserWeb["WebSite (Bibliotecário)"]:::web
    
    subgraph Cloud["-"]
        direction TB
        API["API RestFull"]:::api
        DB[("PostgreSQL")]:::db
        Storage["Supabase Storage"]:::storage
    end
    
    External["Google Books / BrasilAPI"]:::external

    UserMobile -->|REST API / JSON| API
    UserWeb -->|REST API / JSON| API
    
    API -->|JPA / Hibernate| DB
    API -->|Upload Capas e PDF's| Storage
    API -.->|Consulta Metadados| External
```

<br/>

<div align="center">
  <h1>Segurança</h1>
</div>

- **Spring Security & JWT:** Implementação robusta de autenticação e autorização `Stateless`.
- **Criptografia:** Senhas armazenadas com hash BCrypt.
- **CORS Config:** Política de acesso restrita aos domínios da aplicação Web e Mobile.

<br/>

<div align="center">
  <sub>LumiLivre © 2025 - Todos os direitos reservados.</sub>
</div>
