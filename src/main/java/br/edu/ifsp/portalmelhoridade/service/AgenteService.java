package br.edu.ifsp.portalmelhoridade.service;

import br.edu.ifsp.portalmelhoridade.model.ChamadaTool;
import br.edu.ifsp.portalmelhoridade.model.PedidoAgente;
import br.edu.ifsp.portalmelhoridade.model.RespostaAgente;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Orquestrador do Agente Guia Acessível: comando -> interpretação (LLM/Gemini/Fallback) ->
 * validação estrita (ValidadorTools) -> chamadas autorizadas.
 * Quem altera a interface é o navegador; o servidor só devolve chamadas autorizadas e seguras.
 */
@Service
public class AgenteService {

    public static final double LIMIAR_CONFIANCA = 0.80;
    private static final int MAX_TEXTO = 1000;

    private final ProvedorLlm llm;
    private final ValidadorTools validador;

    public AgenteService(ProvedorLlm llm, ValidadorTools validador) {
        this.llm = llm;
        this.validador = validador;
    }

    public RespostaAgente processar(PedidoAgente pedido) {
        String texto = pedido == null || pedido.texto() == null ? "" : pedido.texto().strip();
        if (texto.isEmpty() || texto.length() > MAX_TEXTO) {
            return fa01(0.0, "Não entendi. Escolha uma opção.");
        }

        int nivel = Math.max(ValidadorTools.FONTE_MIN,
                Math.min(ValidadorTools.FONTE_MAX, pedido.nivelFonte()));
        List<String> hist = pedido.historico() != null ? pedido.historico() : List.of();
        ProvedorLlm.Intencao intencao = llm.interpretar(texto, nivel, hist);
        
        if (intencao == null || intencao.confianca() < LIMIAR_CONFIANCA) {
            double conf = intencao != null ? intencao.confianca() : 0.0;
            String msg = (intencao != null && intencao.mensagem() != null && !intencao.mensagem().isBlank())
                    ? intencao.mensagem()
                    : "Não entendi com certeza. Escolha uma das opções abaixo:";
            return fa01(conf, msg);
        }

        // Resposta conversacional direta (sem ferramentas de tela necessárias, ex: saudações, esclarecimentos, dúvidas frequentes)
        if (intencao.chamadas() == null || intencao.chamadas().isEmpty()) {
            String msg = (intencao.mensagem() != null && !intencao.mensagem().isBlank())
                    ? intencao.mensagem()
                    : "Posso ajudar você a encontrar postos de saúde, direitos e benefícios, atividades de lazer e contatos de emergência em Hortolândia.";
            return new RespostaAgente(intencao.confianca(), false, List.of(), List.of(), msg, intencao.origem());
        }

        List<ChamadaTool> autorizadas = new ArrayList<>();
        List<String> bloqueadas = new ArrayList<>();
        for (ChamadaTool chamada : intencao.chamadas()) {
            Optional<String> erro = validador.validar(chamada, nivel);
            if (erro.isPresent()) {
                bloqueadas.add(chamada.name() + ": " + erro.get());
            } else {
                autorizadas.add(chamada);
                if ("ajustarFonte".equals(chamada.name())) {
                    Object deltaObj = chamada.arguments().get("delta");
                    if (deltaObj instanceof Number num) {
                        nivel += num.intValue();
                    }
                }
            }
        }

        if (autorizadas.isEmpty()) {
            return new RespostaAgente(intencao.confianca(), false, autorizadas, bloqueadas, "Não consegui fazer essa ação. Confira o tamanho da fonte ou escolha uma opção na tela.", intencao.origem());
        }

        String msg = (intencao.mensagem() != null && !intencao.mensagem().isBlank())
                ? intencao.mensagem()
                : "Pronto.";

        return new RespostaAgente(intencao.confianca(), false, autorizadas, bloqueadas, msg, intencao.origem());
    }

    private RespostaAgente fa01(double confianca, String mensagem) {
        return new RespostaAgente(confianca, true, List.of(), List.of(), mensagem, "Assistente");
    }
}
