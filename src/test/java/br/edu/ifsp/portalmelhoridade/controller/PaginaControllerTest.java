package br.edu.ifsp.portalmelhoridade.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PaginaControllerTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void paginaInicialEntregaCatalogoParaAInterface() throws Exception {
        mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("window.PMI_CATALOGO")))
                .andExpect(content().string(org.hamcrest.Matchers.anyOf(
                        org.hamcrest.Matchers.containsString("Saúde e Bem-Estar"),
                        org.hamcrest.Matchers.containsString("Sa\\u00FAde e Bem-Estar"))))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"numero\":\"192\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"app\"")));
    }

    @Test
    void apiDoAgenteDevolveChamadasValidadas() throws Exception {
        mvc.perform(post("/api/agente").contentType("application/json")
                        .content("{\"texto\":\"Quero ver os postos de vacinação, mas com letras bem grandes\",\"nivelFonte\":0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fa01").value(false))
                .andExpect(jsonPath("$.chamadas[0].name").value("ajustarFonte"))
                .andExpect(jsonPath("$.chamadas[1].arguments.idSecao").value("saude"));
    }
}
