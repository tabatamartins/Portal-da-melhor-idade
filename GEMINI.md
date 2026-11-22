# Regras do Projeto

## Idioma e Localização (Obrigatório)

- **Comunicação:** Responda sempre em Português (Brasil).
- **Código e Comentários:** Escreva todos os comentários de código, explicações e documentação em português.
- **Controle de Versão:** Escreva todas as mensagens de commit em português.
- **Interface com o Usuário:** Escreva todos os textos de interface (UI), mensagens de validação, alertas e rótulos em português.

---

## Arquitetura e Stack do Projeto

### 1. Ambiente e Versões
- **Linguagem:** Java 17 (com uso intensivo de *Java Records* para DTOs imutáveis, *pattern matching* para `switch` e `instanceof`).
- **Framework:** Spring Boot 3.3.4 (gerenciado via `spring-boot-starter-parent`).
- **Gerenciador de Dependências:** Apache Maven (`pom.xml` – grupo `br.edu.ifsp`, artefato `portal-melhor-idade`, versão `0.1.0`).

### 2. Dependências do Spring Boot
- `spring-boot-starter-web`: Criação e exposição de endpoints REST (`/api/agente`), serialização/deserialização JSON com Jackson, cliente HTTP moderno `RestClient` e servidor Tomcat embarcado.
- `spring-boot-starter-thymeleaf`: Motor de templates server-side para servir a casca HTML (`index.html`) e injetar dados iniciais no frontend.
- `spring-boot-starter-test`: Testes unitários e de integração utilizando JUnit 5 (Jupiter), Assertions e `MockMvc`.
- `spring-boot-maven-plugin`: Plugin Maven para compilação, empacotamento e execução da aplicação Spring Boot.

### 3. Configurações da Aplicação (`application.properties`)
- `spring.application.name=portal-melhor-idade`: Nome de identificação da aplicação.
- `server.port=8080`: Porta HTTP padrão para execução do servidor.
- `server.servlet.encoding.charset=UTF-8` e `server.servlet.encoding.force=true`: Codificação forçada em UTF-8 para suporte consistente à acentuação em português.
- `spring.thymeleaf.cache=false (perfil dev)`: Cache de templates desabilitado para agilidade em tempo de desenvolvimento.
- **Integração com Google Gemini:**
  - `gemini.api.key=${GEMINI_API_KEY:}`: Chave de API do Google AI Studio (fallback automático se ausente).
  - `gemini.model=gemini-3.5-flash-lite`: Modelo de IA utilizado para interpretação semântica com Structured Outputs.
  - `gemini.base-url=https://generativelanguage.googleapis.com/v1beta`: Endpoint da API do Gemini.
  - `gemini.timeout-seconds=6`: Limite de tempo de resposta para chamadas remotas de IA.

### 4. Estrutura de Pacotes (`br.edu.ifsp.portalmelhoridade`)
- **Raiz do Pacote:**
  - `PortalMelhorIdadeApplication.java`: Classe principal de inicialização contendo a anotação `@SpringBootApplication`.
- **`controller`:**
  - `PaginaController.java`: `@Controller` MVC responsável pela rota raiz (`GET /`), injetando o catálogo inicial na view Thymeleaf `index`.
  - `AgenteController.java`: `@RestController` que expõe o endpoint `POST /api/agente`, recebendo `PedidoAgente` e retornando `RespostaAgente`.
- **`model`:**
  - Classes do tipo *Record* que modelam o domínio e os contratos da API de forma imutável:
    - `Catalogo`: Agregador de seções e canais de emergência.
    - `Secao` e `Item`: Estrutura de seções temáticas (`saude`, `direitos`, `emergencia`, `lazer`) e seus respectivos serviços reais (incluindo endereço, telefone, horário, URL oficial e data de atualização).
    - `Emergencia`: Canais públicos de emergência e apoio (SAMU 192, Bombeiros 193, Polícia Militar 190, Disque 100 e Defesa Civil 199).
    - `PedidoAgente`: Payload de entrada com o comando em texto/voz e o nível de fonte atual (`nivelFonte`).
    - `RespostaAgente`: Payload de saída com o nível de confiança, flag do fluxo FA01, listas de ferramentas autorizadas (`chamadas`) e rejeitadas (`bloqueadas`).
    - `ChamadaTool`: Estrutura de chamada de ferramenta (*Tool Calling*) com `name` e mapa de `arguments`.
- **`service`:**
  - `ProvedorLlm`: Interface de contrato para modelos de linguagem, definindo o record `Intencao(confianca, chamadas, mensagem)` e o método `interpretar(texto)`.
  - `GeminiLlmClient`: Provedor `@Primary` que integra com o Google Gemini (gemini-3.5-flash-lite) via `RestClient` do Spring Boot com *Structured Outputs* (JSON Schema estrito). Conta com fallback automático e resiliente para o `LlmSimulado` em caso de chave ausente, falha de rede ou timeout.
  - `LlmSimulado`: Provedor local `@Component("llmSimulado")` para contingência offline via expressões regulares e normalização textual, com proteção nativa contra injeção de prompt.
  - `AgenteService`: Orquestrador do agente inteligente. Valida limites do comando (máximo 300 caracteres), consulta o provedor de IA (`ProvedorLlm`), aplica o limiar de confiança (0,80), ativa o fluxo de contingência FA01 quando necessário e filtra as chamadas autorizadas via `ValidadorTools`.
  - `ValidadorTools`: Componente central de segurança e governança (POL01, RNF02). Aplica whitelist estrita de ferramentas (`ajustarFonte`, `alterarContraste`, `navegarParaSecao`, `executarLeituraVoz`) e valida tipos e intervalos numéricos aceitáveis.
  - `CatalogoService`: Fornecedor de dados persistidos via JPA do catálogo de serviços públicos de Hortolândia/SP e canais de apoio do portal.
- **`src/test/java/br/edu/ifsp/portalmelhoridade`:**
  - `AgenteServiceTest`: Validação de regras de negócio, comandos compostos, detecção de injeção e limites de ferramentas (CT01 a CT08).
  - `CatalogoServiceTest`: Verificação de integridade, links oficiais e alinhamento do catálogo com a whitelist.
  - `PaginaControllerTest`: Testes de integração WebMvc para a view principal e para o endpoint do agente.
  - `GeminiLlmClientTest`: Testes unitários de resiliência, validação de fallback automático sem chave, simulação de falha de conexão HTTP e injeção de prompt.

### 5. Frontend e Padrões de Interface
- **Arquitetura Híbrida (Thymeleaf + SPA Leve):**
  - O arquivo `index.html` atua como casca da aplicação, injetando o catálogo via `window.PMI_CATALOGO`.
  - A interface é controlada pelo script `portal.js` em estilo SPA responsiva com layout adaptável para celular (<640px), tablet (640-1024px) e desktop (>1024px).
- **Acessibilidade Universal (WCAG 2.1 A/AA e e-MAG):**
  - Fonte **Atkinson Hyperlegible** voltada para baixa visão, combinada com ícones **Material Symbols Rounded** e tags semânticas ARIA.
  - Controles de acessibilidade para redimensionamento de fonte (-2 a +4), entrelinhas (`--lh`), alternância de temas (claro/escuro) e modo de alto contraste (`data-contrast="alto"` em preto e amarelo).
  - Suporte a síntese de voz nativa (`speechSynthesis` em `pt-BR`), entrada por reconhecimento de fala (`SpeechRecognition`) e respeito a `prefers-reduced-motion`.
- **Defesa em Profundidade na Execução de Ferramentas:**
  - O backend filtra e valida as ferramentas autorizadas antes da resposta HTTP via `ValidadorTools`.
  - O frontend (`portal.js`) revalida a whitelist localmente antes de alterar qualquer estado do DOM.
- **Resiliência e Fluxo Alternativo (FA01):**
  - Se a confiança for inferior a 0,80, a frase for ambígua ou houver timeout de 8 segundos (RNF03), o sistema aciona o FA01, exibindo botões grandes para que o usuário idoso escolha diretamente a ação sem frustração.


### Banco de Dados e Migrações
- O perfil padrão é `dev` e utiliza H2 em memória, escutando somente em `127.0.0.1`.
- O perfil `prod` utiliza PostgreSQL por meio de `DB_URL`, `DB_USER` e `DB_PASS`.
- As migrações SQL ficam em `src/main/resources/db/migration` e são aplicadas pelo Flyway.
- `spring.jpa.hibernate.ddl-auto=validate`: o Hibernate valida o esquema, mas não altera tabelas automaticamente.
- H2 possui escopo `runtime`; `spring-boot-starter-test` possui escopo `test`.

### Privacidade e Proteção da API
- A chave Gemini é enviada no cabeçalho `x-goog-api-key`, nunca na URL.
- O endpoint `/api/agente` aplica limite básico de 20 solicitações por minuto por endereço IP.
- O campo `origem` da resposta informa se a interpretação veio do Gemini ou do modo simulado.
- O texto pode ser enviado ao Google quando Gemini estiver habilitado; não incluir dados pessoais ou sensíveis.
