package br.edu.ifsp.portalmelhoridade.service;

import br.edu.ifsp.portalmelhoridade.model.ChamadaTool;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Cliente de integração com a API oficial do Google Gemini.
 * Fallback automático para {@link LlmSimulado} se a chave não estiver configurada
 * ou ocorrer qualquer erro de comunicação.
 */
@Service
@Primary
public class GeminiLlmClient implements ProvedorLlm {

    private static final Logger logger = LoggerFactory.getLogger(GeminiLlmClient.class);

    private final ProvedorLlm fallback;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;
    private final String apiKey;
    private final String model;

    @Autowired
    public GeminiLlmClient(
            @Qualifier("llmSimulado") ProvedorLlm fallback,
            ObjectMapper objectMapper,
            RestClient.Builder restClientBuilder,
            @Value("${gemini.api.key:${GEMINI_API_KEY:}}") String apiKey,
            @Value("${gemini.model:${GEMINI_MODEL:gemini-2.0-flash}}") String model,
            @Value("${gemini.base-url:https://generativelanguage.googleapis.com/v1beta}") String baseUrl,
            @Value("${gemini.timeout-seconds:15}") int timeoutSeconds) {

        this.fallback = fallback;
        this.objectMapper = objectMapper;
        this.apiKey = apiKey != null ? apiKey.trim() : "";
        this.model = (model != null && !model.isBlank()) ? model.trim() : "gemini-2.0-flash";

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(5));
        requestFactory.setReadTimeout(Duration.ofSeconds(Math.max(10, timeoutSeconds)));

        this.restClient = restClientBuilder
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();

        logger.info("GeminiLlmClient iniciado. Modelo: {}. Chave configurada: {}",
                this.model, !this.apiKey.isBlank());
    }

    /** Construtor utilitário para testes unitários. */
    public GeminiLlmClient(
            ProvedorLlm fallback,
            ObjectMapper objectMapper,
            String apiKey,
            String model) {
        this(fallback, objectMapper, RestClient.builder(), apiKey, model,
                "https://generativelanguage.googleapis.com/v1beta", 15);
    }

    @Override
    public Intencao interpretar(String texto) {
        return interpretar(texto, 0);
    }

    @Override
    public Intencao interpretar(String texto, int nivelFonte) {
        if (apiKey.isBlank()) {
            logger.warn("Chave GEMINI_API_KEY nao configurada. Usando LlmSimulado.");
            return interpretarFallback(texto, nivelFonte);
        }

        try {
            Map<String, Object> payload = montarPayload(texto, nivelFonte);
            String uri = "/models/" + model + ":generateContent?key=" + apiKey;

            logger.debug("Chamando Gemini: modelo={}, texto={}", model, texto.substring(0, Math.min(50, texto.length())));

            String responseBody = restClient.post()
                    .uri(uri)
                    .header("x-goog-api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(String.class);

            if (responseBody == null || responseBody.isBlank()) {
                logger.warn("Resposta vazia da API do Gemini. Acionando fallback.");
                return interpretarFallback(texto, nivelFonte);
            }

            logger.debug("Resposta Gemini recebida ({} chars)", responseBody.length());

            JsonNode root = objectMapper.readTree(responseBody);

            // Verifica erro retornado pela API
            if (root.has("error")) {
                logger.warn("API Gemini retornou erro: {}", root.path("error").path("message").asText());
                return interpretarFallback(texto, nivelFonte);
            }

            JsonNode textNode = root.path("candidates").path(0).path("content").path("parts").path(0).path("text");
            if (textNode.isMissingNode() || textNode.asText().isBlank()) {
                logger.warn("Estrutura de resposta invalida do Gemini. Acionando fallback.");
                return interpretarFallback(texto, nivelFonte);
            }

            String jsonIntencao = textNode.asText().trim();
            // Remove markdown code fences se presentes
            if (jsonIntencao.startsWith("```")) {
                jsonIntencao = jsonIntencao.replaceAll("^```[a-z]*\\n?", "").replaceAll("\\n?```$", "").trim();
            }

            GeminiRespostaDto dto = objectMapper.readValue(jsonIntencao, GeminiRespostaDto.class);
            List<ChamadaTool> chamadas = dto.chamadas() != null ? dto.chamadas() : List.of();

            String mensagem = (dto.mensagem() != null && !dto.mensagem().isBlank())
                    ? dto.mensagem().trim()
                    : "Entendido! Como mais posso ajudar?";

            double confianca = Math.max(0.0, Math.min(1.0, dto.confianca()));
            if (mensagem.length() > 1500) mensagem = mensagem.substring(0, 1500);

            logger.info("Gemini respondeu com confianca={}, chamadas={}", confianca, chamadas.size());
            return new Intencao(confianca, chamadas, mensagem, "Gemini");

        } catch (Exception ex) {
            logger.warn("Falha na consulta ao Gemini ({}): {}. Acionando fallback.",
                    ex.getClass().getSimpleName(), ex.getMessage());
            return interpretarFallback(texto, nivelFonte);
        }
    }

    private Intencao interpretarFallback(String texto, int nivelFonte) {
        Intencao local = fallback.interpretar(texto, nivelFonte);
        return new Intencao(local.confianca(), local.chamadas(), local.mensagem(), "Simulado (offline)");
    }

    private Map<String, Object> montarPayload(String texto, int nivelFonte) {
        Map<String, Object> systemInstruction = Map.of(
                "parts", List.of(Map.of(
                        "text", """
                                Você é a Assistente Virtual do Portal da Melhor Idade de Hortolândia/SP, chamada "Iris".
                                Você conversa com pessoas idosas de forma carinhosa, clara, simples e respeitosa.
                                
                                PERSONALIDADE:
                                - Seja acolhedora, paciente e gentil como uma neta atenciosa.
                                - Use linguagem simples, sem termos técnicos.
                                - Sempre responda em português do Brasil.
                                - Em saudações e bate-papo, responda de forma natural e amigável.
                                - Pode usar emojis com moderação (😊, ❤️, 🌟).
                                
                                CONHECIMENTOS DO DIA A DIA que você domina:
                                - Saudações e conversas simples: "bom dia", "como vai?", "tudo bem?"
                                - Clima e tempo (oriente a verificar a previsão local)
                                - Dicas de saúde geral para idosos (hidratação, sono, alimentação)
                                - Remédios: oriente sempre a consultar o médico
                                - Receitas simples e dicas de culinária
                                - Feriados e datas comemorativas brasileiras
                                - Direitos do idoso (Estatuto do Idoso, Lei 10.741/2003)
                                - Aposentadoria, INSS, BPC/LOAS
                                - Gratuidade em transporte público
                                - Dicas de bem-estar: exercícios suaves, caminhada, dança
                                - Como usar o celular, apps simples, chamadas de vídeo
                                - Prevenção de golpes e fraudes contra idosos
                                - Convívio familiar e social
                                
                                FERRAMENTAS DO PORTAL (use quando o pedido for para navegar ou ajustar):
                                1. ajustarFonte(delta: int) → Altera o tamanho da fonte. delta de -2 a 2.
                                2. alterarContraste(ativo: boolean) → Ativa/desativa alto contraste.
                                3. navegarParaSecao(idSecao: string) → Vai para: "saude", "direitos", "emergencia" ou "lazer".
                                4. executarLeituraVoz(idSecao: string) → Lê em voz alta a seção.
                                
                                REGRAS IMPORTANTES:
                                - NUNCA invente telefones, endereços ou horários específicos.
                                - Para emergências REAIS (mal estar, acidente), retorne emergencia com alta confiança.
                                - Para bate-papo e perguntas gerais: retorne confianca >= 0.85, chamadas vazias [], e uma mensagem útil e acolhedora.
                                - Para comandos do portal: retorne as ferramentas corretas.
                                - Nível da fonte atual será informado no pedido.
                                """
                ))
        );

        Map<String, Object> userContent = Map.of(
                "role", "user",
                "parts", List.of(Map.of("text", texto + "\nNível atual da fonte: " + nivelFonte
                        + ". Limite: -2 a +4."))
        );

        Map<String, Object> responseSchema = Map.of(
                "type", "OBJECT",
                "properties", Map.of(
                        "confianca", Map.of("type", "NUMBER", "description", "Grau de certeza entre 0.0 e 1.0"),
                        "mensagem", Map.of("type", "STRING", "description", "Resposta acolhedora em português para o usuário idoso"),
                        "chamadas", Map.of(
                                "type", "ARRAY",
                                "description", "Lista de ferramentas a executar (vazia para bate-papo)",
                                "items", Map.of(
                                        "type", "OBJECT",
                                        "properties", Map.of(
                                                "name", Map.of("type", "STRING"),
                                                "arguments", Map.of(
                                                        "type", "OBJECT",
                                                        "properties", Map.of(
                                                                "delta", Map.of("type", "INTEGER"),
                                                                "ativo", Map.of("type", "BOOLEAN"),
                                                                "idSecao", Map.of("type", "STRING")
                                                        )
                                                )
                                        ),
                                        "required", List.of("name", "arguments")
                                )
                        )
                ),
                "required", List.of("confianca", "mensagem", "chamadas")
        );

        Map<String, Object> generationConfig = Map.of(
                "temperature", 0.3,
                "responseMimeType", "application/json",
                "responseSchema", responseSchema
        );

        return Map.of(
                "systemInstruction", systemInstruction,
                "contents", List.of(userContent),
                "generationConfig", generationConfig
        );
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record GeminiRespostaDto(
            double confianca,
            String mensagem,
            List<ChamadaTool> chamadas
    ) {}
}
