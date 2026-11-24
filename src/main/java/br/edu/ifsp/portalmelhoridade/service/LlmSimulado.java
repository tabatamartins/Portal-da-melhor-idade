package br.edu.ifsp.portalmelhoridade.service;

import br.edu.ifsp.portalmelhoridade.model.ChamadaTool;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Provedor de IA local (fallback): converte frases em linguagem natural em chamadas
 * estruturadas de ferramenta, usando regex e regras conversacionais enriquecidas.
 * Atua quando o GeminiLlmClient não está disponível.
 */
@Component("llmSimulado")
public class LlmSimulado implements ProvedorLlm {

    private static final Pattern INJECAO = Pattern.compile(
            "\\b(ignore|ignora|desconsidere|esqueca|apague)\\b.{0,45}\\b(regras|instrucoes|prompt|sistema)\\b" +
            "|\\b(system prompt|jailbreak|prompt injection)\\b");

    // --- Acessibilidade ---
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
    private static final Pattern LER = Pattern.compile(
            "\\b(le|ler|leia|ouvir|ouca|fala|falar|escuta|escutar)\\b" +
            "|le pra mim|leia pra mim|ouvir informacoes");

    // --- Seções do portal ---
    private static final Pattern SEC_LAZER = Pattern.compile(
            "lazer|atividade|oficina|danca|convivencia|convivio|clube|" +
            "ccmi|centro.*convivencia|transporte|onibus|" +
            "cartao.*idoso|passe.?livre|gratuid.*(transporte|onibus|passagem)|" +
            "(transporte|onibus).*gratuid|ginastica|caminhada|pilates|zumba|teatro|passeio|excursao|recreacao");
    private static final Pattern SEC_SAUDE = Pattern.compile(
            "vacin|saude|posto|ubs|upa|remedio|medic|consulta|" +
            "agendamento|sadt|rosolem|enfermeira|enfermeiro|" +
            "atendimento medico|pronto.?socorro|hospital|farmacia|exame");
    private static final Pattern SEC_DIREITOS = Pattern.compile(
            "direito|direitos|estatuto|benefic|gratuid(?!.*(transporte|onibus|passagem))|" +
            "bpc|loas|cras|cadunico|cadastro.?unico|" +
            "assistencia|bolsa|auxilio|pensao|inss|aposentadori|previdencia|amanda");
    private static final Pattern SEC_EMERGENCIA = Pattern.compile(
            "emergen|samu|socorro|policia|bombeiro|urgente|urgencia|" +
            "acidente|mal estar|mal.?me.?sinto|passando mal|dor no peito|caiu|queda|desmai|" +
            "chama.*socorro|ligo.*policia|\\b192\\b|\\b193\\b|\\b190\\b|\\b199\\b|\\b100\\b");

    private static final Map<String, String> MENSAGEM_SEC = Map.of(
            "saude",      "Abrindo Saúde com postos, UPAs e serviços de Hortolândia! 🏥",
            "direitos",   "Abrindo Direitos e Assistência Social — CRAS, BPC e benefícios. ⚖️",
            "emergencia", "Abrindo Emergências. Se for urgência, ligue direto pelo número na tela! 🚨",
            "lazer",      "Abrindo Lazer e Transporte — atividades do CCMI e gratuidade de ônibus. 🎉"
    );

    @Override
    public Intencao interpretar(String texto) { return interpretar(texto, 0); }

    @Override
    public Intencao interpretar(String texto, int nivelFonte) {
        String t = normalizar(texto);

        // Proteção contra injeção de prompt
        if (INJECAO.matcher(t).find()) {
            return new Intencao(0.1, List.of(), "Esse pedido não pôde ser processado por segurança.");
        }

        // ============================================================
        // CONVERSAS DO DIA A DIA
        // ============================================================

        // Saudações
        if (t.matches(".*\\b(oi|ola|bom dia|boa tarde|boa noite|e ai|tudo bem|tudo bom|opa|alo|hey|boa|salve)\\b.*")) {
            String hora = saudacaoHoraria();
            return ok(hora + "! Que bom te ver por aqui 😊 Sou a Iris, assistente do Portal da Melhor Idade de Hortolândia. Como posso te ajudar hoje?");
        }

        // Como vai / estado do usuário
        if (t.matches(".*\\b(como vai|como voce ta|como esta voce|tudo bem|tudo certo|ta bem|esta bem|e voce)\\b.*")) {
            return ok("Estou ótima, obrigada por perguntar! 😊 E você, como está se sentindo? Posso ajudar com saúde, direitos, lazer ou qualquer dúvida do dia a dia.");
        }

        // Quem é você / apresentação
        if (t.matches(".*\\b(quem e voce|o que voce faz|como funciona|quem criou|para que serve|como me ajuda|voce e um robo|e uma ia|inteligencia artificial)\\b.*")) {
            return ok("Sou a Iris, assistente virtual do Portal da Melhor Idade de Hortolândia! 🌟 Posso ajudar com saúde, direitos, atividades de lazer, emergências e muito mais. Pode me perguntar à vontade!");
        }

        // Agradecimentos
        if (t.matches(".*\\b(obrigado|obrigada|valeu|muito obrigado|muito obrigada|agradeco|agradecida|que otimo|que bom|perfeito|maravilhoso|excelente)\\b.*")) {
            return ok("Que ótimo! Fico muito feliz em ajudar 😊 Se precisar de mais alguma coisa, é só me chamar!");
        }

        // Despedida
        if (t.matches(".*\\b(tchau|ate logo|ate mais|ate breve|adeus|bye|boa noite|tenha um bom|cuida se|fui)\\b.*")) {
            return ok("Tchau! Cuide-se bem e qualquer dúvida é só me chamar 😊 Tenha um ótimo dia!");
        }

        // ============================================================
        // SAÚDE DO DIA A DIA
        // ============================================================

        // Dor / mal-estar (não emergência)
        if (t.matches(".*\\b(dor de cabeca|dor de barriga|enjoo|nausea|tontura|cansaco|cansada|cansado|fraqueza|dor nas costas|dor no joelho|dor muscular)\\b.*")
                && !SEC_EMERGENCIA.matcher(t).find()) {
            return ok("Sentindo-se mal? 😟 Para dores e mal-estar, o ideal é consultar seu médico ou visitar a UBS mais próxima. Quer que eu mostre os postos de saúde de Hortolândia?");
        }

        // Pressão alta / diabetes
        if (t.matches(".*\\b(pressao alta|hipertensao|pressao baixa|diabetes|glicose|insulina|diabetico)\\b.*")) {
            return new Intencao(0.9, List.of(new ChamadaTool("navegarParaSecao", Map.of("idSecao", "saude"))),
                    "Para controle de pressão e diabetes, acompanhamento regular na UBS é fundamental! Abri a seção de saúde com os postos de Hortolândia. 🏥");
        }

        // Remédio / medicamento
        if (t.matches(".*\\b(remedio|medicamento|farmacia popular|remedios|posologia|tomar remedio|esqueci.{0,15}remedio)\\b.*")) {
            return new Intencao(0.9, List.of(new ChamadaTool("navegarParaSecao", Map.of("idSecao", "saude"))),
                    "Medicamentos básicos podem ser retirados com receita na UBS. Nunca altere doses sem orientação médica! Abri a seção de saúde. 💊");
        }

        // Vacinas
        if (t.matches(".*\\b(vacina|vacinacao|gripe|covid|influenza|pneumonia|vacinar)\\b.*")) {
            return new Intencao(0.9, List.of(new ChamadaTool("navegarParaSecao", Map.of("idSecao", "saude"))),
                    "A vacinação para a terceira idade é feita em todas as UBSs de Hortolândia, de segunda a sexta. Abri a lista de postos na tela! 💉");
        }

        // Sono / dormir
        if (t.matches(".*\\b(dormir|sono|insonia|nao durmo|nao consigo dormir|dormindo mal|acordando cedo|cansado de dia)\\b.*")) {
            return ok("Dificuldade para dormir é bem comum! 😴 Algumas dicas: evite celular e TV antes de dormir, tome um chá de camomila, e mantenha horários regulares. Se persistir, consulte seu médico na UBS.");
        }

        // Alimentação / dieta
        if (t.matches(".*\\b(comer|alimentacao|dieta|emagrecer|engordei|peso|nutricionista|frutas|verduras|agua|hidratacao)\\b.*")) {
            return ok("Uma boa alimentação faz toda a diferença! 🥗 Priorize frutas, verduras, e beba bastante água (pelo menos 6 copos por dia). Para orientação personalizada, consulte o nutricionista na UBS de Hortolândia.");
        }

        // Exercício físico
        if (t.matches(".*\\b(exercicio|atividade fisica|caminhar|caminhar|academia|alongamento|fisioterapia|fisioterapeuta)\\b.*")
                && !SEC_LAZER.matcher(t).find()) {
            return ok("Exercícios são ótimos para a saúde! 🚶 Caminhadas leves de 30 minutos por dia já fazem muito bem. O CCMI de Hortolândia oferece ginástica, dança e hidroginástica. Quer ver as atividades?");
        }

        // ============================================================
        // SEGURANÇA E GOLPES
        // ============================================================

        if (t.matches(".*\\b(golpe|fraude|estelionato|enganaram|roubaram|clonaram|falso|mentira|golpista|bandido ligou|banco ligou|cartao clonado)\\b.*")) {
            return ok("⚠️ Muito cuidado com golpes! Bancos NUNCA pedem senha por telefone ou WhatsApp. Se receber ligação suspeita, desligue e ligue de volta para o número oficial do seu banco. Em caso de crime, ligue 190 (Polícia).");
        }

        if (t.matches(".*\\b(senha|cartao|conta bancaria|pix|transferencia|deposito)\\b.{0,30}\\b(pedir|pedindo|solicitando|enviar|entregar)\\b.*")) {
            return ok("⚠️ ATENÇÃO! Nunca forneça senha, cartão ou faça transferências por pedido de terceiros. Isso é sinal de golpe! Ligue para seu banco pelo número do cartão e comunique à polícia pelo 190.");
        }

        // ============================================================
        // TECNOLOGIA E CELULAR
        // ============================================================

        if (t.matches(".*\\b(celular|telefone|smartphone|aplicativo|app|whatsapp|videochamada|chamada de video|internet|wifi|dados)\\b.*")
                && !SEC_EMERGENCIA.matcher(t).find()) {
            return ok("Precisa de ajuda com o celular? 📱 Posso te orientar! Qual é a sua dúvida? Seja WhatsApp, ligação, fotos ou aplicativos — pode me perguntar à vontade.");
        }

        // ============================================================
        // TEMPO / CLIMA
        // ============================================================

        if (t.matches(".*\\b(chuva|sol|clima|tempo|temperatura|vai chover|previsao do tempo|faz calor|ta frio)\\b.*")) {
            return ok("Para saber a previsão do tempo de Hortolândia, você pode verificar no seu celular: abra o Google e pesquise \"tempo em Hortolândia hoje\" 🌤️");
        }

        // ============================================================
        // DATAS E FERIADOS
        // ============================================================

        if (t.matches(".*\\b(feriado|folga|dia de|aniversario|natal|pascoa|carnaval|dia das maes|dia dos pais|dia do idoso)\\b.*")) {
            return ok("📅 Posso ajudar com datas comemorativas! O Dia do Idoso é em 1º de outubro. Para feriados municipais de Hortolândia, você pode verificar no site da prefeitura ou me perguntar uma data específica.");
        }

        // ============================================================
        // DIREITOS DO IDOSO (conversa, não navegação)
        // ============================================================

        if (t.matches(".*\\b(estatuto.{0,15}idoso|lei.{0,15}idoso|direito.{0,15}idoso|60 anos|terceira idade|melhor idade)\\b.*")
                && !SEC_DIREITOS.matcher(t).find()) {
            return new Intencao(0.9, List.of(new ChamadaTool("navegarParaSecao", Map.of("idSecao", "direitos"))),
                    "O Estatuto do Idoso (Lei 10.741/2003) garante prioridade em serviços, saúde, transporte gratuito e proteção contra discriminação. Abri a seção de Direitos para você! ⚖️");
        }

        // Aposentadoria / INSS
        if (t.matches(".*\\b(aposentadoria|inss|previdencia|pensao|beneficio|bpc|loas|auxilio)\\b.*")) {
            return new Intencao(0.9, List.of(new ChamadaTool("navegarParaSecao", Map.of("idSecao", "direitos"))),
                    "Para dúvidas sobre aposentadoria e benefícios, o CRAS de Hortolândia pode orientar presencialmente. Abri a seção de Direitos com mais informações! 📋");
        }

        // ============================================================
        // LAZER E BEM-ESTAR (conversa)
        // ============================================================

        if (t.matches(".*\\b(entediado|tediosa|sozinho|sozinha|solidao|nao tenho o que fazer|tempo livre|me divertir|passatempo)\\b.*")) {
            return new Intencao(0.9, List.of(new ChamadaTool("navegarParaSecao", Map.of("idSecao", "lazer"))),
                    "Que tal conhecer as atividades do CCMI? 🎉 Dança, ginástica, coral, artesanato e muito mais — tudo no Centro de Convivência da Melhor Idade de Hortolândia! Abri as opções de lazer.");
        }

        // ============================================================
        // TELEFONES DE EMERGÊNCIA
        // ============================================================

        if (t.matches(".*\\b(telefone|numero|contato|ligar)\\b.{0,20}\\b(samu|ambulancia)\\b.*") ||
                t.matches(".*\\b(samu|ambulancia)\\b.*\\b(telefone|numero)\\b.*")) {
            return new Intencao(0.95, List.of(new ChamadaTool("navegarParaSecao", Map.of("idSecao", "emergencia"))),
                    "O SAMU é o 192 — gratuito, funciona 24 horas! 🚑 Abri os números de emergência na tela.");
        }
        if (t.matches(".*\\b(telefone|numero|contato|ligar)\\b.{0,20}\\b(policia|pm|viatura)\\b.*") ||
                t.matches(".*\\b(policia|pm)\\b.*\\b(telefone|numero)\\b.*")) {
            return new Intencao(0.95, List.of(new ChamadaTool("navegarParaSecao", Map.of("idSecao", "emergencia"))),
                    "A Polícia Militar atende pelo 190 — gratuito! 🚔 Abri a seção de emergência.");
        }
        if (t.matches(".*\\b(bombeiro|bombeiros|fogo|incendio)\\b.*")) {
            return new Intencao(0.95, List.of(new ChamadaTool("navegarParaSecao", Map.of("idSecao", "emergencia"))),
                    "Bombeiros: ligue 193 — gratuito, 24 horas! 🚒 Abri os números de emergência na tela.");
        }
        if (t.matches(".*\\b(disque 100|direitos humanos|denuncia.{0,20}idoso|violencia.{0,20}idoso)\\b.*")) {
            return new Intencao(0.95, List.of(new ChamadaTool("navegarParaSecao", Map.of("idSecao", "emergencia"))),
                    "Disque 100: canal gratuito para denúncias de violência e proteção ao idoso. 📞 Abri a seção de emergência.");
        }
        if (t.matches(".*\\b(defesa civil)\\b.*")) {
            return new Intencao(0.95, List.of(new ChamadaTool("navegarParaSecao", Map.of("idSecao", "emergencia"))),
                    "A Defesa Civil de Hortolândia atende pelo 199. Abri a seção de emergência. 🛡️");
        }
        if (t.matches(".*\\b(guarda municipal|gm|seguranca municipal)\\b.*")) {
            return new Intencao(0.95, List.of(new ChamadaTool("navegarParaSecao", Map.of("idSecao", "emergencia"))),
                    "A Guarda Municipal de Hortolândia atende pelo 153. Abri a seção de emergência. 🛡️");
        }

        // Vaga de idoso / credencial estacionamento
        if (t.matches(".*\\b(vaga.{0,15}idoso|estacionamento.{0,15}idoso|credencial.{0,15}estacionamento|cartao.{0,15}estacionamento)\\b.*")) {
            return ok("A credencial para vaga de estacionamento reservada à pessoa idosa (a partir de 60 anos) pode ser solicitada no setor de trânsito da Prefeitura ou pelo Poupatempo. 🚗");
        }

        // Agendamento de consultas
        if (t.matches(".*\\b(agendar.{0,15}consulta|marcar.{0,15}consulta|como marco consulta|como agendo consulta)\\b.*")) {
            return new Intencao(0.9, List.of(new ChamadaTool("navegarParaSecao", Map.of("idSecao", "saude"))),
                    "O agendamento de consultas é realizado na sua UBS de referência em Hortolândia com cartão SUS e documento com foto. Abri os postos de saúde na tela! 🏥");
        }

        // Hospital e UPAs
        if (t.matches(".*\\b(hospital|upa|pronto atendimento|urgencia)\\b.*")) {
            return new Intencao(0.9, List.of(new ChamadaTool("navegarParaSecao", Map.of("idSecao", "saude"))),
                    "Hortolândia conta com o Hospital Municipal Mário Covas e as UPAs 24h (Jd. Amanda, Jd. Rosolém e Santa Clara). Abri a lista de saúde para você! 🏥");
        }

        // Cartão transporte / passe livre
        if (t.matches(".*\\b(cartao.{0,15}onibus|bilhete.{0,15}unico|passe livre|gratuidade.{0,15}onibus|como ter passe)\\b.*")) {
            return new Intencao(0.9, List.of(new ChamadaTool("navegarParaSecao", Map.of("idSecao", "lazer"))),
                    "Idosos a partir de 60 anos têm direito à gratuidade no transporte de Hortolândia com o Cartão Bem Idoso. Abri as informações de transporte e lazer! 🚌");
        }

        // Pedido de ajuda geral
        if (t.matches(".*\\b(ajuda|me ajuda|ajudar|socorro|duvida|orientacao|explicar|o que posso pedir|opcoes)\\b.*")
                && !SEC_EMERGENCIA.matcher(t).find()) {
            return ok("Estou aqui para te ajudar! 😊 Você pode me pedir para: 🏥 Ver postos de saúde, 🔠 Aumentar as letras, 🚨 Ver telefones de emergência, 🚌 Ônibus e lazer ou me fazer qualquer pergunta sobre Hortolândia!");
        }

        // Voltar ao início
        if (t.matches(".*\\b(inicio|pagina inicial|home|começo|voltar ao inicio)\\b.*")) {
            return new Intencao(0.95, List.of(new ChamadaTool("navegarParaSecao", Map.of("idSecao", "home"))),
                    "Voltando para a página inicial do Portal! 🏠");
        }

        // Normalizar fonte
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
            if (ajustes.isEmpty()) return ok("A fonte já está no tamanho normal.");
            return new Intencao(0.9, ajustes, "Voltando a fonte ao tamanho normal. ✅");
        }

        // ============================================================
        // COMANDOS DO PORTAL (padrão)
        // ============================================================

        List<ChamadaTool> chamadas = new ArrayList<>();
        int acertos = 0;

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

        if (CONTRASTE_OFF.matcher(t).find()) {
            chamadas.add(new ChamadaTool("alterarContraste", Map.of("ativo", false)));
            acertos++;
        } else if (CONTRASTE_ON.matcher(t).find()) {
            chamadas.add(new ChamadaTool("alterarContraste", Map.of("ativo", true)));
            acertos++;
        }

        List<String> secoes = detectarSecoes(t);
        boolean ler = LER.matcher(t).find();

        if (!secoes.isEmpty()) {
            for (String secao : secoes) {
                chamadas.add(new ChamadaTool("navegarParaSecao", Map.of("idSecao", secao)));
                acertos++;
                if (ler) chamadas.add(new ChamadaTool("executarLeituraVoz", Map.of("idSecao", secao)));
            }
        } else if (ler) {
            chamadas.clear();
            acertos = 0;
        }

        double confianca = acertos > 0 ? 0.9 : 0.85;
        String mensagem = acertos > 0
                ? (!secoes.isEmpty() ? MENSAGEM_SEC.getOrDefault(secoes.get(0), "Pronto! Ação realizada. ✅") : "Pronto! Ação realizada. ✅")
                : "Posso te ajudar com postos de saúde, direitos e benefícios, transporte, lazer e telefones de emergência em Hortolândia! Me diga o que você procura ou escolha uma das opções abaixo 😊";

        return new Intencao(confianca, chamadas, mensagem);
    }

    private static Intencao ok(String msg) {
        return new Intencao(0.9, List.of(), msg);
    }

    private static String saudacaoHoraria() {
        int hora = java.time.LocalTime.now().getHour();
        if (hora < 12) return "Bom dia";
        if (hora < 18) return "Boa tarde";
        return "Boa noite";
    }

    private static List<String> detectarSecoes(String t) {
        List<String> resultado = new ArrayList<>();
        if (SEC_EMERGENCIA.matcher(t).find()) resultado.add("emergencia");
        if (SEC_LAZER.matcher(t).find()) resultado.add("lazer");
        if (SEC_SAUDE.matcher(t).find()) resultado.add("saude");
        if (SEC_DIREITOS.matcher(t).find()) resultado.add("direitos");
        return resultado;
    }

    static String normalizar(String s) {
        String semAcento = Normalizer.normalize(s == null ? "" : s, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        String limpo = semAcento.toLowerCase().replaceAll("[^a-z0-9\\s]", " ");
        // Reduz letras repetidas 3+ vezes para 1 (ex: "poooosto" -> "posto", "ajudaaa" -> "ajuda")
        limpo = limpo.replaceAll("([a-z])\\1{2,}", "$1");

        // Normalização fonética e correção de erros comuns de digitação de idosos
        limpo = (" " + limpo + " ")
                .replaceAll("\\bnum\\s+to\\b", "nao estou")
                .replaceAll("\\bnao\\s+to\\b", "nao estou")
                .replaceAll("\\bta\\b", "esta")
                .replaceAll("\\bto\\b", "estou")
                .replaceAll("\\bmt[o]?\\b", "muito")
                .replaceAll("\\bqr[o]?\\b", "quero")
                .replaceAll("\\bvr\\b", "ver")
                .replaceAll("\\bpq\\b", "porque")
                .replaceAll("\\b(kd|cade)\\b", "onde esta")
                .replaceAll("\\bfika\\b", "fica")
                .replaceAll("\\b(possto|postu|psto)\\b", "posto")
                .replaceAll("\\b(onbus|onibs|omibus)\\b", "onibus")
                .replaceAll("\\b(remedyo|remedo)\\b", "remedio")
                .replaceAll("\\b(vassina|vacinaa)\\b", "vacina")
                .replaceAll("\\b(direitu|direto)\\b", "direito")
                .replaceAll("\\bsocoro\\b", "socorro")
                .replaceAll("\\bpolica\\b", "policia")
                .replaceAll("\\bbombero\\b", "bombeiro")
                .replaceAll("\\bhospitau\\b", "hospital")
                .replaceAll("\\bajuda\\s+eu\\b", "me ajuda")
                .replaceAll("\\b(faso|fasso)\\b", "faco");

        return limpo.replaceAll("\\s+", " ").trim();
    }
}
