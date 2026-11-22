# Melhorias aplicadas ao Portal da Melhor Idade

## Alterações implementadas
- A chave do Gemini passou da URL para o cabeçalho `x-goog-api-key`; logs não registram a mensagem de exceção, que poderia conter dados sensíveis.
- O modelo padrão foi atualizado para `gemini-3.5-flash-lite`; o modelo pode ser alterado por `GEMINI_MODEL`.
- O tempo limite remoto foi reduzido para no máximo 7 segundos (3 s de conexão + 4 s de leitura).
- O schema de resposta passou a declarar as propriedades aceitas em `arguments`.
- O nível atual da fonte é enviado ao Gemini. A validação limita cada ajuste a ±2 e respeita os limites gerais.
- A resposta informa o provedor (`Gemini` ou `Simulado (offline)`). O texto livre gerado pelo Gemini não é reproduzido na tela/voz; a confirmação é fixa.
- O fallback reconhece frases comuns de aumento/redução, negações, desativação de contraste, termos de emergência, singular de “direito” e retorno da fonte ao tamanho normal.
- O falso positivo de injeção para frases como “executar exame” foi removido; instruções explícitas de manipulação continuam bloqueadas.
- Foi adicionado limite básico de 20 chamadas por minuto por endereço IP ao endpoint `/api/agente`.
- A tela Ajuda informa que o texto pode ser enviado ao Google e remove alegações sem comprovação de “alta precisão” e de eliminação de alucinações.
- O projeto usa H2 no perfil padrão `dev` (somente loopback) e PostgreSQL ao ativar `prod`.
- Duplicidades de configuração foram removidas; Hibernate usa `ddl-auto=validate` e Flyway cria/versiona o esquema, incluindo chave estrangeira de `itens.secao_id`.
- Escopos Maven foram corrigidos, `.gitignore` foi adicionado e arquivos de build gerados foram removidos do pacote.
- Google Fonts foi removido da página; ícones têm alternativas locais para a demonstração sem internet.
- README e GEMINI.md foram atualizados. O caso de teste CT03 foi renumerado e foram adicionados 12 testes; o projeto passa a conter 39 testes automatizados no total. O Trello precisa ser atualizado manualmente.

## Itens que ainda exigem validação ou trabalho manual
- O caminho real do Gemini ainda precisa ser testado com uma chave válida e conexão com a internet.
- A confiança continua sendo fornecida pelo modelo; whitelist, tipos e limites são validados, mas isso não prova a intenção semântica.
- O limite por IP é uma proteção básica em memória, não substitui autenticação, rate limiting distribuído ou controles de custo no provedor.
- A interface SPA ainda não apresenta duas seções simultaneamente quando uma frase pede várias; o backend pode retornar mais de uma navegação, mas a última tela prevalece.
- O catálogo, endereços, telefones, horários e datas precisam de conferência humana em fontes oficiais. Nenhum dado de contato foi inventado ou “validado” por esta alteração.
- As datas continuam como texto; o `DataInitializer` ainda só popula tabelas vazias.
- O arquivo `portal.js` ainda pode receber uma formatação completa; o Trello precisa ser atualizado manualmente. O Maven Wrapper não foi gerado porque Maven não está instalado neste ambiente.
- Voz, microfone, aparência em navegadores e inicialização completa do Spring não foram testados nesta execução.

## Verificações realizadas
- Compilação isolada das classes centrais Java (`LlmSimulado`, `ValidadorTools` e `AgenteService`): OK.
- Verificação sintática do `portal.js` com Node: OK.
- Leitura do `pom.xml` como XML: OK.
- `mvn test` e inicialização completa não foram executados: Maven não está instalado neste ambiente.
