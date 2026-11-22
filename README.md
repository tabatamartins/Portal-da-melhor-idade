# Portal da Melhor Idade

Aplicação web em Java 17, Spring Boot 3.3.4, Thymeleaf e JPA.

## Como executar localmente

1. Instale Java 17 e Maven.
2. Na pasta do projeto, execute:

   ```bash
   mvn spring-boot:run
   ```

3. Acesse http://localhost:8080. O perfil padrão `dev` utiliza H2 em memória e escuta somente em `127.0.0.1`.

Para executar os testes: `mvn test`.

## Banco de dados

- Desenvolvimento: H2 em memória, configurado em `application-dev.properties`. Os dados são recriados a cada inicialização.
- Produção: ative `--spring.profiles.active=prod` para usar PostgreSQL e configure `DB_URL`, `DB_USER` e `DB_PASS` no ambiente antes de iniciar.
- O esquema é versionado pelo Flyway em `src/main/resources/db/migration`. O Hibernate usa `ddl-auto=validate`; não atualiza tabelas automaticamente.

## Preparação para hospedagem em nuvem (Docker / Render)

O projeto inclui um `Dockerfile` para gerar a imagem da aplicação com Java 17. O container inicia com o perfil `prod`.

Configure no serviço de hospedagem as variáveis abaixo (não coloque valores secretos no Git):

- `DB_URL`: URL JDBC completa do PostgreSQL, no formato `jdbc:postgresql://HOST:5432/NOME_DO_BANCO`.
- `DB_USER`: usuário do banco PostgreSQL.
- `DB_PASS`: senha do banco PostgreSQL.
- `GEMINI_API_KEY`: opcional; chave do Google AI Studio.
- `PORT`: opcional; a plataforma normalmente fornece essa variável automaticamente.

No Render, crie um serviço do tipo **Web Service** conectado ao repositório Git e escolha **Docker** como ambiente. Crie também um PostgreSQL e copie host, porta, nome do banco, usuário e senha para montar `DB_URL`, `DB_USER` e `DB_PASS`. Use a conexão interna do banco quando o serviço estiver na mesma região.

O perfil `prod` exige as três variáveis do PostgreSQL. Se estiverem ausentes ou incorretas, a aplicação não inicia; isso evita publicar acidentalmente usando H2 em memória. A primeira inicialização executa as migrações Flyway automaticamente.

Antes de publicar, execute `mvn test` localmente e confira se os dados, telefones, endereços e horários do catálogo estão corretos. Não use dados pessoais reais nos testes do agente.

## Integração Gemini

A integração é opcional. Configure `GEMINI_API_KEY` no ambiente para habilitá-la. O modelo pode ser alterado por `GEMINI_MODEL` (padrão: `gemini-3.5-flash-lite`). Sem chave ou em caso de falha, o sistema usa `LlmSimulado`. A resposta da API inclui o campo `origem` para identificar o provedor utilizado.

**Privacidade:** quando o Gemini está habilitado, o texto enviado pelo usuário é transmitido ao Google para interpretação. Não envie dados pessoais ou sensíveis. O assistente não deve ser usado para diagnóstico, aconselhamento médico ou emergências.

## Estrutura principal

- `service/GeminiLlmClient`: integração remota e fallback.
- `service/LlmSimulado`: interpretação local por regras.
- `service/ValidadorTools`: whitelist e validação das ferramentas.
- `service/AgenteService`: orquestração das intenções.
- `service/CatalogoService`: consulta o catálogo via JPA.
- `controller/AgenteController`: endpoint `POST /api/agente`.

O conteúdo de serviços públicos precisa ser conferido diretamente nos canais oficiais antes de uma publicação.
