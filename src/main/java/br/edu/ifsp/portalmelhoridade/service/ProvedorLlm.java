package br.edu.ifsp.portalmelhoridade.service;

import br.edu.ifsp.portalmelhoridade.model.ChamadaTool;

import java.util.List;

/**
 * Contrato de integração para provedores de inteligência artificial do Portal.
 * Define a estrutura imutável de Intencao retornada pelo modelo de linguagem.
 */
public interface ProvedorLlm {

    /**
     * Intenção interpretada a partir do comando da pessoa idosa.
     *
     * @param confianca Grau de certeza entre 0.0 e 1.0.
     * @param chamadas  Lista de ferramentas estruturadas (Tool Calling) sugeridas.
     * @param mensagem  Mensagem acolhedora, clara e em português para sintetizar em voz ou exibir na tela.
     */
    record Intencao(double confianca, List<ChamadaTool> chamadas, String mensagem, String origem) {
        public Intencao(double confianca, List<ChamadaTool> chamadas) {
            this(confianca, chamadas, null, "não informado");
        }
        public Intencao(double confianca, List<ChamadaTool> chamadas, String mensagem) {
            this(confianca, chamadas, mensagem, "não informado");
        }
    }

    /**
     * Interpreta o comando em linguagem natural enviado pelo usuário.
     *
     * @param texto Comando digitado ou transcrito de voz.
     * @return Intencao contendo confiança, ferramentas e mensagem explicativa.
     */
    Intencao interpretar(String texto);

    /** Permite ao provedor considerar o estado atual da interface quando necessário. */
    default Intencao interpretar(String texto, int nivelFonte) {
        return interpretar(texto);
    }
}
