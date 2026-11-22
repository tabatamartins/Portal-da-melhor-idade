package br.edu.ifsp.portalmelhoridade.model;

import java.util.List;

/** Todo o conteúdo do portal, entregue à página como JSON (window.PMI_CATALOGO). */
public record Catalogo(List<Secao> secoes, List<Emergencia> emergencias) { }
