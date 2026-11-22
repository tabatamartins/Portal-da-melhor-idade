package br.edu.ifsp.portalmelhoridade.service;

import br.edu.ifsp.portalmelhoridade.model.Emergencia;
import br.edu.ifsp.portalmelhoridade.model.Item;
import br.edu.ifsp.portalmelhoridade.model.Secao;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes de integração do catálogo — roda com H2 em memória via
 * src/test/resources/application.properties, sem precisar de PostgreSQL.
 */
@SpringBootTest
class CatalogoServiceTest {

    @Autowired
    private CatalogoService catalogo;

    @Test
    void secoesDoCatalogoSaoAsMesmasDaWhitelistDoValidador() {
        List<String> ids = catalogo.secoes().stream().map(Secao::id).toList();
        assertEquals(ValidadorTools.SECOES, java.util.Set.copyOf(ids));
    }

    @Test
    void emergenciaTemCincoCanaisEAsOutrasSecoesTemQuatroItens() {
        assertEquals(5, catalogo.emergencias().size());
        for (Secao s : catalogo.secoes()) {
            if (s.id().equals("emergencia")) assertTrue(s.itens().isEmpty());
            else assertEquals(4, s.itens().size(), s.id());
        }
    }

    @Test
    void transporteVaiParaTransporteELazerComoNoPrototipo() {
        var r = new AgenteService(new LlmSimulado(), new ValidadorTools())
                .processar(new br.edu.ifsp.portalmelhoridade.model.PedidoAgente("quero saber do ônibus", 0));
        assertEquals("lazer", r.chamadas().get(0).arguments().get("idSecao"));
    }

    @Test
    void todosOsItensPossuemCamposOficiaisPreenchidos() {
        for (Secao secao : catalogo.secoes()) {
            for (Item item : secao.itens()) {
                assertNotNull(item.icone(),           "Ícone não pode ser nulo");
                assertNotNull(item.titulo(),          "Título não pode ser nulo");
                assertNotNull(item.descricao(),       "Descrição não pode ser nula");
                assertNotNull(item.texto(),           "Texto não pode ser nulo");

                assertNotNull(item.endereco(),        "Endereço não pode ser nulo");
                assertFalse(item.endereco().isBlank(),"Endereço não pode ser vazio");

                assertNotNull(item.telefone(),        "Telefone não pode ser nulo");
                assertFalse(item.telefone().isBlank(),"Telefone não pode ser vazio");

                assertNotNull(item.horario(),         "Horário não pode ser nulo");
                assertFalse(item.horario().isBlank(), "Horário não pode ser vazio");

                assertNotNull(item.fonteOficial(),    "Fonte oficial não pode ser nula");
                assertTrue(item.fonteOficial().startsWith("http"), "Fonte oficial deve ser URL válida");

                assertNotNull(item.dataAtualizacao(), "Data de atualização não pode ser nula");
                assertFalse(item.dataAtualizacao().isBlank(), "Data de atualização não pode ser vazia");
            }
        }
    }

    @Test
    void canaisDeEmergenciaPossuemNumerosOficiais() {
        var numeros = catalogo.emergencias().stream().map(Emergencia::numero).toList();
        assertEquals(Set.of("192", "193", "190", "100", "199"), Set.copyOf(numeros));
    }
}
