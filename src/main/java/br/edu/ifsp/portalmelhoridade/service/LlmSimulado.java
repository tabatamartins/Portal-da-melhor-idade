package br.edu.ifsp.portalmelhoridade.service;

import br.edu.ifsp.portalmelhoridade.model.ChamadaTool;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Provedor de IA local (fallback): converte frases em linguagem natural em chamadas
 * estruturadas de ferramenta (Tool Calling) por meio de expressões regulares e
 * normalização fonética/textual.
 *
 * <p>Atua como mecanismo de contingência quando o {@link GeminiLlmClient} não possui
 * chave configurada ou está temporariamente indisponível.</p>
 *
 * <p>Os padrões foram enriquecidos com sinônimos regionais de Hortolândia/SP
 * (ex.: "rosolen", "cras", "bpc", "gratuidade onibus", "ccmi") para cobrir
 * o vocabulário natural da população idosa local.</p>
 */
@Component("llmSimulado")
public class LlmSimulado implements ProvedorLlm {

    // Bloqueia apenas instruções explícitas de manipulação do assistente; não bloqueia
    // palavras comuns como "executar exame".
    private static final Pattern INJECAO = Pattern.compile(
            "\\b(ignore|ignora|desconsidere|esqueca|apague)\\b.{0,45}\\b(regras|instrucoes|prompt|sistema)\\b" +
            "|\\b(system prompt|jailbreak|prompt injection)\\b");

    private static final Pattern AUMENTAR = Pattern.compile(
            "(letras?|fonte|texto).{0,25}(pequen|miud|dificil de ler|nao consigo ler)" +
            "|(aument|maior|ampli).{0,20}(letra|fonte|texto)|letras? (bem )?(grand|maior)|muito pequen|letra pequena|nao consigo ler");
    private static final Pattern DIMINUIR = Pattern.compile(
            "(letra|fonte|texto).{0,25}(esta |ta |ficou )?(muito )?(grand|gigant|enorme)" +
            "|(diminu|menor|reduz).{0,20}(letra|fonte|texto)|muito grand|grandes? demais");

    private static final Pattern CONTRASTE_OFF = Pattern.compile(
            "(deslig|desativ|tir|remov|normaliz).{0,25}contraste" +
            "|(nao quero|nao use|sem).{0,20}(alto )?contraste");
    private static final Pattern CONTRASTE_ON = Pattern.compile(
            "(ativ|lig).{0,20}contraste|alto contraste|contraste alto|" +
            "nao (to |estou )?enxerg|nao enxergo|vista cansad|dificuldade.{0,15}ver");

    // --- Leitura em voz ---
    private static final Pattern LER = Pattern.compile(
            "\\b(le|ler|leia|ouvir|ouca|fala|falar|escuta|escutar)\\b" +
            "|le pra mim|leia pra mim|ouvir informacoes");

    // --- Lazer/Transporte (verificado ANTES de direitos para evitar conflito com "gratuid") ---
    // Cobre: lazer, atividade, CCMI, convivência, dança, curso, transporte, ônibus, gratuidade
    // de transporte especificamente, cartão idoso, passe livre, clube, ginástica, caminhada
    private static final Pattern SEC_LAZER = Pattern.compile(
            "lazer|atividade|oficina|danca|convivencia|convivio|clube|" +
            "ccmi|centro.*convivencia|" +
            "transporte|onibus|" +
            "cartao.*idoso|passe.?livre|" +
            "gratuid.*(transporte|onibus|passagem)|" +
            "(transporte|onibus).*gratuid|" +
            "ginastica|caminhada|pilates|zumba|teatro|passeio|excursao|recreacao");

    // --- Saúde: NÃO inclui "amanda" aqui (evita conflito com CRAS Amanda) ---
    // Cobre: postos, UBS, UPA, vacinação, remédio, consulta, Rosolém, agendamento, SADT
    private static final Pattern SEC_SAUDE = Pattern.compile(
            "vacin|saude|posto|ubs|upa|remedio|medic|consulta|" +
            "agendamento|sadt|rosolem|enfermeira|enfermeiro|" +
            "atendimento medico|pronto.?socorro|hospital|farmacia|exame");

    // --- Direitos e Assistência Social ---
    // Cobre: direitos, estatuto, CRAS, LOAS, BPC, CadÚnico, bolsa, assistência, INSS
    private static final Pattern SEC_DIREITOS = Pattern.compile(
            "direito|direitos|estatuto|benefic|gratuid(?!.*(transporte|onibus|passagem))|" +
            "bpc|loas|cras|cadunico|cadastro.?unico|" +
            "assistencia|bolsa|auxilio|pensao|inss|aposentadori|previdencia|amanda");

    // --- Emergência ---
    // Cobre: emergência, SAMU, socorro, polícia, bombeiro, ajuda urgente, números de emergência
    private static final Pattern SEC_EMERGENCIA = Pattern.compile(
            "emergen|samu|socorro|policia|bombeiro|urgente|urgencia|" +
            "acidente|mal estar|mal.?me.?sinto|passando mal|dor no peito|caiu|queda|desmai|chama.*socorro|ligo.*policia|" +
            "\\b192\\b|\\b193\\b|\\b190\\b|\\b199\\b|\\b100\\b");

    // --- Mensagens contextuais por seção (nunca null — usa "sem-secao" como chave segura) ---
    private static final Map<String, String> MENSAGEM_SECAO = Map.of(
            "saude",      "Abrindo a seção de Saúde com postos, UPAs e serviços de saúde de Hortolândia.",
            "direitos",   "Abrindo a seção de Direitos e Assistência Social com informações sobre CRAS, BPC e benefícios.",
            "emergencia", "Abrindo os canais de emergência. Se for urgência, ligue direto pelo número exibido na tela.",
            "lazer",      "Abrindo a seção de Lazer e Transporte com atividades do CCMI e informações de gratuidade."
    );

    @Override
    public Intencao interpretar(String texto) {
        return interpretar(texto, 0);
    }

    @Override
    public Intencao interpretar(String texto, int nivelFonte) {
        String t = normalizar(texto);

        // Proteção contra injeção de prompt: retorna confiança muito baixa.
        if (INJECAO.matcher(t).find()) {
            return new Intencao(0.1, List.of(), "Esse comando não pôde ser processado por segurança.");
        }

        // Saudações acolhedoras
        if (t.matches(".*\\b(oi|ola|bom dia|boa tarde|boa noite|e ai|tudo bem|opa|alo)\\b.*")) {
            return new Intencao(0.95, List.of(), "Olá! Tudo bem? Sou seu assistente do Portal da Melhor Idade de Hortolândia. Como posso te ajudar hoje? Posso mostrar postos de saúde, direitos, atividades de lazer e telefones úteis.");
        }

        // Quem é você / Apresentação
        if (t.matches(".*\\b(quem e voce|o que voce faz|como funciona|quem criou|para que serve|como me ajuda|ajuda com o que)\\b.*")) {
            return new Intencao(0.95, List.of(), "Sou o assistente virtual do Portal da Melhor Idade de Hortolândia! Posso te ajudar a encontrar postos de saúde, consultar direitos e benefícios, ver atividades no CCMI e acessar números de emergência.");
        }

        // Agradecimentos
        if (t.matches(".*\\b(obrigado|obrigada|valeu|muito obrigado|muito obrigada|agradeco)\\b.*")) {
            return new Intencao(0.95, List.of(), "De nada! Fico muito feliz em ajudar. Se precisar de mais alguma informação de Hortolândia, é só me chamar!");
        }

        // Dúvidas de telefones específicos
        if (t.matches(".*\\b(telefone|numero|contato|ligar)\\b.{0,20}\\b(samu|ambulancia)\\b.*") || t.matches(".*\\b(samu|ambulancia)\\b.*\\b(telefone|numero)\\b.*")) {
            return new Intencao(0.95, List.of(new ChamadaTool("navegarParaSecao", Map.of("idSecao", "emergencia"))), "O telefone do SAMU é 192 (ligação gratuita, 24 horas). Abri os números de emergência na sua tela.");
        }
        if (t.matches(".*\\b(telefone|numero|contato|ligar)\\b.{0,20}\\b(policia|pm|viatura)\\b.*") || t.matches(".*\\b(policia|pm)\\b.*\\b(telefone|numero)\\b.*")) {
            return new Intencao(0.95, List.of(new ChamadaTool("navegarParaSecao", Map.of("idSecao", "emergencia"))), "O telefone da Polícia Militar é 190. Abri a seção de emergência na tela.");
        }
        if (t.matches(".*\\b(telefone|numero|contato|ligar)\\b.{0,20}\\b(bombeiro|bombeiros|fogo)\\b.*") || t.matches(".*\\b(bombeiro|bombeiros)\\b.*\\b(telefone|numero)\\b.*")) {
            return new Intencao(0.95, List.of(new ChamadaTool("navegarParaSecao", Map.of("idSecao", "emergencia"))), "O telefone do Corpo de Bombeiros é 193. Abri a seção de emergência na tela.");
        }
        if (t.matches(".*\\b(disque 100|direitos humanos|denuncia)\\b.*")) {
            return new Intencao(0.95, List.of(new ChamadaTool("navegarParaSecao", Map.of("idSecao", "emergencia"))), "O Disque Direitos Humanos é o número 100, canal gratuito para orientação e denúncias de proteção ao idoso.");
        }
        if (t.matches(".*\\b(defesa civil)\\b.*")) {
            return new Intencao(0.95, List.of(new ChamadaTool("navegarParaSecao", Map.of("idSecao", "emergencia"))), "A Defesa Civil de Hortolândia atende pelo número 199.");
        }

        // Dúvidas frequentes de Hortolândia
        if (t.matches(".*\\b(vacina|vacinacao|gripe|covid)\\b.*")) {
            return new Intencao(0.95, List.of(new ChamadaTool("navegarParaSecao", Map.of("idSecao", "saude"))), "A vacinação para a terceira idade é realizada em todas as UBSs de Hortolândia de segunda a sexta-feira. Abri a lista de postos na tela para você.");
        }
        if (t.matches(".*\\b(remedio|medicamento|farmacia popular|remedios)\\b.*")) {
            return new Intencao(0.95, List.of(new ChamadaTool("navegarParaSecao", Map.of("idSecao", "saude"))), "Medicamentos básicos e de uso contínuo podem ser retirados com receita médica e Cartão SUS nas farmácias das UBSs de Hortolândia.");
        }
        if (t.matches(".*\\b(carteira|carteirinha|passe livre|transporte gratuito|onibus gratuito)\\b.*")) {
            return new Intencao(0.95, List.of(new ChamadaTool("navegarParaSecao", Map.of("idSecao", "direitos"))), "Pessoas com 60 anos ou mais têm gratuidade nos ônibus de Hortolândia. A carteira pode ser solicitada no CRAS com RG, CPF e comprovante de residência.");
        }
        if (t.matches(".*\\b(ccmi|centro de convivencia|danca|ginastica|oficina|hidroginastica|coral|artesanato)\\b.*")) {
            return new Intencao(0.95, List.of(new ChamadaTool("navegarParaSecao", Map.of("idSecao", "lazer"))), "O Centro de Convivência da Melhor Idade (CCMI) oferece dança, ginástica e oficinas no Remanso Campineiro e no Jd. Amanda. Abri as informações na tela!");
        }
        if (t.matches(".*\\b(inicio|voltar para o inicio|pagina inicial|home)\\b.*")) {
            return new Intencao(0.95, List.of(new ChamadaTool("navegarParaSecao", Map.of("idSecao", "home"))), "Voltando para a página inicial do Portal da Melhor Idade.");
        }

        if (t.matches(".*\\b(voltar|deixar|colocar|normalizar)\\b.{0,25}\\b(normal|padrao)\\b.*")
                || t.matches(".*\\b(fonte|letra|tamanho)\\b.{0,20}\\b(normal|padrao)\\b.*")) {
            int nivel = Math.max(-2, Math.min(4, nivelFonte));
            List<ChamadaTool> ajustes = new ArrayList<>();
            int restante = -nivel;
            while (restante != 0) {
                int passo = Math.max(-2, Math.min(2, restante));
                ajustes.add(new ChamadaTool("ajustarFonte", Map.of("delta", passo)));
                restante -= passo;
            }
            if (ajustes.isEmpty()) return new Intencao(0.3, List.of(), "A fonte já está no tamanho normal.");
            return new Intencao(0.9, ajustes, "Voltando a fonte ao tamanho normal.");
        }

        List<ChamadaTool> chamadas = new ArrayList<>();
        int acertos = 0;

        // Ajuste de tamanho de fonte
        boolean negacaoAumentar = t.matches(".*\\b(nao|nunca)\\b.{0,20}\\b(aument\\w*|ampli\\w*)\\b.*");
        boolean negacaoDiminuir = t.matches(".*\\b(nao|nunca)\\b.{0,20}\\b(diminu\\w*|reduz\\w*)\\b.*");
        if (!negacaoAumentar && AUMENTAR.matcher(t).find()) {
            int delta = t.contains("pouquinho") && t.matches(".*\\b(letra|fonte|texto)\\b.*") ? 1 : 2;
            chamadas.add(new ChamadaTool("ajustarFonte", Map.of("delta", delta)));
            acertos++;
        } else if (!negacaoDiminuir && DIMINUIR.matcher(t).find()) {
            chamadas.add(new ChamadaTool("ajustarFonte", Map.of("delta", -1)));
            acertos++;
        }

        // Contraste
        if (CONTRASTE_OFF.matcher(t).find()) {
            chamadas.add(new ChamadaTool("alterarContraste", Map.of("ativo", false)));
            acertos++;
        } else if (CONTRASTE_ON.matcher(t).find()) {
            chamadas.add(new ChamadaTool("alterarContraste", Map.of("ativo", true)));
            acertos++;
        }

        // Navegação por seção + leitura em voz
        List<String> secoes = detectarSecoes(t);
        boolean ler = LER.matcher(t).find();

        if (!secoes.isEmpty()) {
            for (String secao : secoes) {
                chamadas.add(new ChamadaTool("navegarParaSecao", Map.of("idSecao", secao)));
                acertos++;
                if (ler) chamadas.add(new ChamadaTool("executarLeituraVoz", Map.of("idSecao", secao)));
            }
        } else if (ler) {
            // "Lê pra mim" sem destino claro: ambíguo — não adivinha (POL02)
            chamadas.clear();
            acertos = 0;
        }

        double confianca = acertos > 0 ? 0.9 : 0.3;
        // Usa chave segura: se secao for null, usa mensagem genérica
        String mensagem = acertos > 0
                ? (!secoes.isEmpty() ? MENSAGEM_SECAO.get(secoes.get(0)) : "Pronto! Ação realizada.")
                : "Não entendi com certeza. Escolha uma das opções na tela.";

        return new Intencao(confianca, chamadas, mensagem);
    }

    /**
     * Ordem de prioridade: Lazer antes de Direitos (evita conflito com "gratuid"),
     * Emergência antes de Saúde (palavras como "socorro" não devem ir para saúde).
     */
    private static List<String> detectarSecoes(String t) {
        List<String> resultado = new ArrayList<>();
        if (SEC_EMERGENCIA.matcher(t).find()) resultado.add("emergencia");
        if (SEC_LAZER.matcher(t).find()) resultado.add("lazer");
        if (SEC_SAUDE.matcher(t).find()) resultado.add("saude");
        if (SEC_DIREITOS.matcher(t).find()) resultado.add("direitos");
        return resultado;
    }

    /** Remove acentos, pontuação, converte para minúsculas e normaliza espaços. */
    static String normalizar(String s) {
        String semAcento = Normalizer.normalize(s == null ? "" : s, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        String limpo = semAcento.toLowerCase().replaceAll("[^a-z0-9\\s]", " ");
        return limpo.replaceAll("\\s+", " ").trim();
    }
}
