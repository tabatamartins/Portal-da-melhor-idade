package br.edu.ifsp.portalmelhoridade.model;

import java.util.Map;

/** Chamada estruturada de ferramenta (Tool Calling): nome + argumentos. */
public record ChamadaTool(String name, Map<String, Object> arguments) { }
