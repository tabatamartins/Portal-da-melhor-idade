package br.edu.ifsp.portalmelhoridade.model;

import java.util.List;

/** Resposta do agente e identificação do provedor que interpretou o comando. */
public record RespostaAgente(double confianca,
                             boolean fa01,
                             List<ChamadaTool> chamadas,
                             List<String> bloqueadas,
                             String mensagem,
                             String origem) {
    public RespostaAgente(double confianca, boolean fa01, List<ChamadaTool> chamadas,
                          List<String> bloqueadas, String mensagem) {
        this(confianca, fa01, chamadas, bloqueadas, mensagem, "não informado");
    }
}
