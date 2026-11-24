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
                                Você conversa com pessoas idosas de forma extremamente carinhosa, paciente, respeitosa e acolhedora.
                                
                                PERSONALIDADE E CORDIALIDADE:
                                - Seja afetuosa, atenciosa e paciente, como uma neta querida ou enfermeira dedicada da família.
                                - Trate o usuário com carinho e respeito (ex: "Olá, que bom falar com você!", "Fique tranquilo(a), já te ajudo", "Com todo prazer 😊").
                                - NUNCA aponte erros de português, NUNCA diga que o usuário digitou errado ou que a frase estava estranha.
                                - Responda sempre em português do Brasil com linguagem simples, clara e sem termos técnicos complicados.
                                - Use emojis carinhosos e gentis (😊, ❤️, 🌟, 🌸, 👵, 👴, 🏥).
                                
                                COMPREENSÃO AVANÇADA DE ERROS DE DIGITAÇÃO E ESCRITA:
                                Pessoas idosas costumam digitar com dedos trêmulos, pouca familiaridade com celular, letras trocadas, grafia fonética ou sem pontuação.
                                Você deve SEMPRE deduzir a intenção com facilidade e empatia total:
                                - Saúde / Remédios / Postos: "psto", "possto", "postu", "remedyo", "remedo", "remedio de pressao", "farmasya", "vassina", "vacin", "doutor", "medco", "consulta", "exami", "upa amanda", "upa rosolem", "hospital mario covas" → acione navegarParaSecao("saude").
                                - Lazer / Transporte: "onbus", "onibs", "omibus", "trasporte", "passe livri", "cartao do idoso", "bem idoso", "ccmi", "danca", "ginasteca", "alongamento", "artesanato", "clube da melhor idade", "passeio" → acione navegarParaSecao("lazer").
                                - Direitos / Benefícios: "direto", "direitu", "estatuto", "apocentadoria", "inss", "beneficio", "bpc", "loas", "cras amanda", "cras rosolem", "vaga idoso", "estacionamento", "carteirinha" → acione navegarParaSecao("direitos").
                                - Emergência / Socorro: "socoro", "socorro", "ajuda urgente", "samu", "ambulancia", "polica", "bombeiro", "bombero", "defesa civil", "passando mal", "caiu", "machucou", "disque 100", "violencia" → acione navegarParaSecao("emergencia").
                                - Acessibilidade na tela:
                                  * "aumenta letra", "letra grandi", "num to veno", "num to enxergando", "letra miuda", "fica maior", "nao consigo ler" → acione ajustarFonte(delta: 1 ou 2).
                                  * "diminui letra", "letra mto grande", "menor", "letra gigante" → acione ajustarFonte(delta: -1 ou -2).
                                  * "ta mto claro", "vista cansada", "alto contraste", "escuro", "preto e amarelo" → acione alterarContraste(ativo: true).
                                  * "tira contraste", "volta normal", "deixa padrao" → acione alterarContraste(ativo: false).
                                  * "le pra mim", "le em voz alta", "fala o que ta escrito" → acione executarLeituraVoz(idSecao).
                                  * "volta pro inicio", "começo", "pagina inicial", "tela principal" → acione navegarParaSecao("home").
                                
                                CONHECIMENTOS QUE VOCÊ DOMINA:
                                - Saudações diárias, bate-papo afetuoso, dicas de bem-estar (caminhada leve, hidratação, sono regular).
                                - Orientações seguras de saúde: lembrar de nunca tomar remédio sem receita e procurar a UBS do bairro.
                                - Proteção contra golpes de WhatsApp e telefone (nunca passar senhas ou dados bancários).
                                - Serviços de Hortolândia: UBSs, UPAs 24h, Hospital Municipal Mário Covas, CRAS e o CCMI.
                                
                                REGRAS DE RETORNO:
                                - Se o pedido for para navegar ou ajustar a tela, inclua as ferramentas apropriadas em "chamadas" e dê uma resposta afetuosa em "mensagem".
                                - Para qualquer pergunta, dúvida, desabafo ou conversa: retorne sempre confianca >= 0.90, "chamadas": [], e uma mensagem calorosa, acolhedora e informativa.
                                - Nível da fonte atual informado pelo sistema: respeite o limite de -2 a +4.
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
