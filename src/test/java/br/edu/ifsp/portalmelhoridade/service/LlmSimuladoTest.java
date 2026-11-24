package br.edu.ifsp.portalmelhoridade.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LlmSimuladoTest {
    private final LlmSimulado llm = new LlmSimulado();

    @Test void letraPequenaAumentaFonte() {
        var r = llm.interpretar("a letra está pequena");
        assertEquals(2, r.chamadas().get(0).arguments().get("delta"));
    }

    @Test void letrasGrandesSolicitadasAumentamFonte() {
        var r = llm.interpretar("quero letras bem grandes");
        assertEquals(2, r.chamadas().get(0).arguments().get("delta"));
    }


    @Test void pedidoDeAumentoPequenoUsaUmNivel() {
        var r = llm.interpretar("aumenta um pouquinho a letra");
        assertEquals(1, r.chamadas().get(0).arguments().get("delta"));
    }

    @Test void naoConsigoLerAumentaFonte() {
        var r = llm.interpretar("não consigo ler");
        assertEquals(2, r.chamadas().get(0).arguments().get("delta"));
    }

    @Test void fonteGrandeDemaisDiminuiFonte() {
        var r = llm.interpretar("a letra está muito grande");
        assertEquals(-1, r.chamadas().get(0).arguments().get("delta"));
    }

    @Test void comandoNegadoNaoExecutaAjuste() {
        var r = llm.interpretar("não aumente a letra");
        assertTrue(r.chamadas().isEmpty());
    }

    @Test void desativarContrasteDesliga() {
        var r = llm.interpretar("desligar o alto contraste");
        assertEquals(Boolean.FALSE, r.chamadas().get(0).arguments().get("ativo"));
    }

    @Test void exameDeSangueNaoEhInjecao() {
        var r = llm.interpretar("preciso executar um exame de sangue");
        assertTrue(r.confianca() >= 0.8);
        assertTrue(r.chamadas().stream().anyMatch(c -> "navegarParaSecao".equals(c.name())));
    }

    @Test void fraseComEmergenciaAbreSecao() {
        var r = llm.interpretar("minha vizinha caiu");
        assertTrue(r.chamadas().stream().anyMatch(c ->
                "navegarParaSecao".equals(c.name()) &&
                "emergencia".equals(c.arguments().get("idSecao"))));
    }


    @Test void voltarFonteAoNormalGeraAjustesEmEtapasSeguras() {
        var r = llm.interpretar("voltar a fonte ao normal", 4);
        assertEquals(2, r.chamadas().size());
        assertEquals(-2, r.chamadas().get(0).arguments().get("delta"));
        assertEquals(-2, r.chamadas().get(1).arguments().get("delta"));
    }

    @Test void multiplasSecoesSaoReconhecidas() {
        var r = llm.interpretar("quero informações de saúde e depois emergência");
        long navegacoes = r.chamadas().stream()
                .filter(c -> "navegarParaSecao".equals(c.name())).count();
        assertEquals(2, navegacoes);
    }

    @Test void saudacaoRetornaAltaConfiancaEMensagemAcolhedora() {
        var r = llm.interpretar("olá, bom dia!");
        assertTrue(r.confianca() >= 0.8);
        assertTrue(r.mensagem().contains("Portal da Melhor Idade"));
    }

    @Test void perguntaTelefoneSamuAbreEmergenciaComMensagemClara() {
        var r = llm.interpretar("qual o telefone do samu?");
        assertTrue(r.confianca() >= 0.8);
        assertTrue(r.mensagem().contains("192"));
        assertEquals("emergencia", r.chamadas().get(0).arguments().get("idSecao"));
    }
}
